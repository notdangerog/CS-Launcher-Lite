/*
 * CS Launcher Lite — e4mc tunnel binding
 *
 * JNI declaration set for {@code libcs_e4mc.so}, the tunnel library built by the CS Engine CI and
 * used by CS Launcher Plus, (C) CRAFT STUDIO.
 *
 * <p><b>Why this class lives in a {@code com.craftstudio} package.</b> A JNI function that is not
 * registered through {@code RegisterNatives} is looked up by a symbol mangled from the declaring
 * class's fully-qualified name — {@code Java_com_craftstudio_cslauncher_e4mc_E4mcNative_nativeStart}.
 * Keeping this declaration at the exact FQN the prebuilt library exports is what lets a
 * {@code libcs_e4mc.so} built elsewhere drop into {@code jniLibs/arm64-v8a/} and link unchanged,
 * instead of requiring a rebuild against this launcher's package names. The CS Launcher Lite logic
 * that calls it lives in {@code com.movtery.zalithlauncher.host.E4mcBridge}; this file is the ABI
 * contract and nothing more.
 *
 * <p>The library is <b>not</b> part of this repository. Without it every method here is unreachable
 * and {@link #load()} returns false, which {@code E4mcBridge} turns into a clear "this build has no
 * tunnel library" state rather than a crash. See {@code docs/WORLD_HOST.md}.
 */
package com.craftstudio.cslauncher.e4mc;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * JNI binding of {@code libcs_e4mc.so}: publishes a local TCP port under a public
 * {@code *.e4mc.link} address through the e4mc relay network (QUIC), so a Java "Open to LAN" world
 * is reachable by friends on any network — no VPN, no port forwarding, no extra permissions.
 */
public final class E4mcNative {

    private static volatile boolean sLoaded;
    @Nullable
    private static volatile String sLoadError;

    private E4mcNative() {}

    /**
     * Loads the library once.
     *
     * @return false, with {@link #loadError()} explaining why, when this build ships no e4mc library
     */
    public static boolean load() {
        if (sLoaded) return true;
        synchronized (E4mcNative.class) {
            if (sLoaded) return true;
            try {
                System.loadLibrary("cs_e4mc");
                sLoaded = true;
            } catch (UnsatisfiedLinkError | SecurityException e) {
                sLoadError = e.getMessage();
            }
            return sLoaded;
        }
    }

    /** Why {@link #load()} failed, or null when it has not been tried or did not fail. */
    @Nullable
    public static String loadError() {
        return sLoadError;
    }

    /** Starts publishing local port {@code port}; 0 on failure, see {@link #nativeError()}. */
    public static native long nativeStart(int port, @Nullable String relayOrNull);

    /** The assigned public address ({@code name.e4mc.link}), or null while still connecting. */
    @Nullable
    public static native String nativeDomain(long handle);

    /** A problem reported by the tunnel, or null when healthy. */
    @Nullable
    public static native String nativeProblem(long handle);

    /** Guests currently connected through the tunnel. */
    public static native int nativeGuests(long handle);

    /** Ends the tunnel. Safe to call once; the handle is dead afterwards. */
    public static native void nativeStop(long handle);

    /**
     * Who the relay admits from now on.
     *
     * @param maxPlayers         guests at once, host excluded; 0 means no ceiling at the relay
     * @param allowedNamesOrNull newline-separated names, or null for "anyone"
     * @param reason             shown to a guest the name list keeps out
     */
    public static native void nativeSetPolicy(long handle, int maxPlayers,
                                              @Nullable String allowedNamesOrNull,
                                              @Nullable String reason);

    /** Names the host removed, newline-separated (null for none): refused however open the world is. */
    public static native void nativeSetBlocked(long handle, @Nullable String blockedOrNull);

    /** Guests connected right now, one name per line in login order; null when none. */
    @Nullable
    public static native String nativeGuestNames(long handle);

    /** Ends the connection of the guest logged in as {@code name}; returns how many streams ended. */
    public static native int nativeKick(long handle, @NonNull String name);

    /** Refused guests since the last call, one {@code name<TAB>reason} per line; null when none. */
    @Nullable
    public static native String nativePoll(long handle);

    /** The last error reported by the native side, or null. */
    @Nullable
    public static native String nativeError();
}
