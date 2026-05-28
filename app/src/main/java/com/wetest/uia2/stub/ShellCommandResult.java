package com.wetest.uia2.stub;

public class ShellCommandResult {
    public String stdout;
    public String stderr;
    public int returnCode;

    public ShellCommandResult(String stdout, String stderr, int returnCode) {
        this.stdout = stdout;
        this.stderr = stderr;
        this.returnCode = returnCode;
    }

    @Override
    public String toString() {
        return "ShellCommandResult{" +
                "stdout='" + stdout + '\'' +
                ", stderr='" + stderr + '\'' +
                ", returnCode=" + returnCode +
                '}';
    }
}