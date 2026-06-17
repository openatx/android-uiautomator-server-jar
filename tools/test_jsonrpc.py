#!/usr/bin/env python3
"""JSON-RPC test tool for android-uiautomator-server.

Usage:
  python3 test_jsonrpc.py                   # interactive mode
  python3 test_jsonrpc.py ping              # direct call
  python3 test_jsonrpc.py click 100 200     # with args (auto-typed: int, float, bool, JSON, str)
"""

import json
import os
import subprocess
import sys
import urllib.request

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
METHODS_FILE = os.path.join(SCRIPT_DIR, "methods.json")
PORT = 9008
URL = f"http://localhost:{PORT}/jsonrpc/0"

R = "\033[0;31m"
G = "\033[0;32m"
Y = "\033[1;33m"
B = "\033[0;34m"
C = "\033[0;36m"
W = "\033[1m"
N = "\033[0m"


# ---------------------------------------------------------------------------
# helpers
# ---------------------------------------------------------------------------

def read_methods() -> dict:
    with open(METHODS_FILE) as f:
        return json.load(f)


def parse_arg(raw: str):
    """Try JSON, then bool, int, float, else keep as string."""
    s = raw.strip()
    if s.lower() == "true":
        return True
    if s.lower() == "false":
        return False
    if s.lower() == "null":
        return None
    try:
        return json.loads(s)
    except (json.JSONDecodeError, ValueError):
        pass
    try:
        return int(s)
    except ValueError:
        pass
    try:
        return float(s)
    except ValueError:
        pass
    return s


# ---------------------------------------------------------------------------
# adb forward
# ---------------------------------------------------------------------------

def setup_forward():
    print(f"{C}adb forward tcp:{PORT} tcp:{PORT}{N}")
    subprocess.run(["adb", "forward", f"tcp:{PORT}", f"tcp:{PORT}"], check=True)
    print(f"{G}Forward ready.{N}\n")


# ---------------------------------------------------------------------------
# JSON-RPC call
# ---------------------------------------------------------------------------

def jsonrpc_call(method: str, params=None):
    if params is None:
        params = []
    body = json.dumps({"jsonrpc": "2.0", "id": 1, "method": method, "params": params})

    print(f"\n{Y}>>> Request:{N}")
    print(json.dumps(json.loads(body), indent=2))

    try:
        req = urllib.request.Request(
            URL,
            data=body.encode("utf-8"),
            headers={"Content-Type": "application/json"},
        )
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read().decode("utf-8"))

        print(f"{Y}<<< Response:{N}")
        if "error" in data:
            print(f"{R}{json.dumps(data['error'], indent=2, ensure_ascii=False)}{N}")
        elif "result" in data:
            print(json.dumps(data["result"], indent=2, ensure_ascii=False))
        else:
            print(json.dumps(data, indent=2, ensure_ascii=False))
    except urllib.error.URLError as e:
        print(f"{R}Connection error: {e}{N}")
    except Exception as e:
        print(f"{R}{e}{N}")


# ---------------------------------------------------------------------------
# interactive mode
# ---------------------------------------------------------------------------

def pick(items: list, prompt: str) -> int | None:
    """Show numbered list, return chosen index (0-based) or None."""
    for i, label in enumerate(items, 1):
        print(f"  {C}{i}{N}. {label}")
    print(f"  {C}0{N}. Quit")

    while True:
        try:
            s = input(f"\n{C}{prompt}{N} ").strip()
            if s.lower() in ("q", "quit", "0"):
                return None
            n = int(s)
            if 1 <= n <= len(items):
                return n - 1
        except (ValueError, EOFError):
            pass
        print(f"{R}Invalid choice{N}")


def category_loop():
    methods_data = read_methods()
    cat_map = methods_data["categories"]
    cat_ids = list(cat_map.keys())
    methods_all = methods_data["methods"]

    print(f"{B}{'=' * 40}{N}")
    print(f"{W}  JSON-RPC Test Tool{N}  (port {PORT})")
    print(f"{B}{'=' * 40}{N}")

    while True:
        # ---- choose category ----
        labels = [cat_map[c] for c in cat_ids]
        idx = pick(labels, "Category:")
        if idx is None:
            print("Bye!")
            return
        category = cat_ids[idx]

        # ---- choose method ----
        entries = [m for m in methods_all if m["category"] == category]
        if not entries:
            print(f"{R}No methods in this category.{N}")
            input("Press Enter...")
            continue

        print(f"\n{W}--- {cat_map[category]} ---{N}")
        for i, m in enumerate(entries, 1):
            pnames = ", ".join(p["name"] for p in m["params"])
            pstr = f"({pnames})" if pnames else ""
            print(f"  {C}{i:>2}{N}. {G}{m['name']:<30}{N} {Y}{pstr}{N}")
            print(f"       {m['description']}")

        midx = pick(
            [f"{m['name']} {('(' + ','.join(p['name'] for p in m['params']) + ')') if m['params'] else ''}"
             for m in entries],
            "Method [b=back]:",
        )
        if midx is None:
            continue

        _call_with_prompt(entries[midx])

        input(f"\n{C}Enter to continue...{N} ")


def _call_with_prompt(method_def: dict):
    name = method_def["name"]
    params = method_def["params"]
    example = method_def.get("example", [])

    print(f"\n{W}>>> {name}{N}")
    print(f"    {method_def['description']}")

    if not params:
        jsonrpc_call(name)
        return

    print(f"\n{Y}Parameters:{N}")
    for p in params:
        print(f"  {p['name']} ({p['type']})")
    print(f"{Y}Example:{N} {json.dumps(example)}")
    print(f"{Y}Enter params as Python list/dict, e.g.{N} {json.dumps(example)}")

    while True:
        raw = input(f"\n{C}Params:{N} ").strip()
        if raw == "":
            args = example
            break
        try:
            # Use ast.literal_eval for safe python-style input (supports dicts too)
            import ast

            val = ast.literal_eval(raw)
            if isinstance(val, (list, tuple)):
                args = list(val)
            else:
                args = [val]
            break
        except (ValueError, SyntaxError):
            # fallback: treat entire input as a single string
            args = [raw]
            break

    jsonrpc_call(name, args)


# ---------------------------------------------------------------------------
# entry
# ---------------------------------------------------------------------------

def main():
    if len(sys.argv) > 1 and sys.argv[1] in ("-h", "--help"):
        print(__doc__)
        sys.exit(0)

    # prerequisites
    try:
        subprocess.run(["adb", "version"], capture_output=True, check=True)
    except (FileNotFoundError, subprocess.CalledProcessError):
        print(f"{R}Error: 'adb' not found in PATH{N}")
        sys.exit(1)
    if not os.path.exists(METHODS_FILE):
        print(f"{R}Error: {METHODS_FILE} not found{N}")
        sys.exit(1)

    setup_forward()

    if len(sys.argv) == 1:
        try:
            category_loop()
        except KeyboardInterrupt:
            print(f"\n{Y}Bye!{N}")
    else:
        method = sys.argv[1]
        params = [parse_arg(a) for a in sys.argv[2:]]
        jsonrpc_call(method, params)


if __name__ == "__main__":
    main()
