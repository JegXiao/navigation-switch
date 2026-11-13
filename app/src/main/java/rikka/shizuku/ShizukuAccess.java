package rikka.shizuku;

import java.lang.Process;

import moe.shizuku.server.IRemoteProcess;

/**
 * Shizuku 13.1.5 exposes neither newProcess() nor ShizukuRemoteProcess's constructor as public
 * API. Living in the library's package lets us call both without reflection; the calls are
 * still checked at compile time against the pinned dependency version.
 */
public final class ShizukuAccess {

    private ShizukuAccess() {
    }

    public static Process newProcess(String[] cmd, String[] env, String dir) throws Exception {
        IRemoteProcess remote = Shizuku.requireService().newProcess(cmd, env, dir);
        return new ShizukuRemoteProcess(remote);
    }
}