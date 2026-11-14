/*
 * CS Launcher Lite — Minecraft: Java Edition host agent
 * Copyright (C) 2025-2026 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * The shape-recognition approach — identify the integrated player list by its bytecode rather than
 * by mapped names, and hook the 26.x publish methods so "Open to LAN" is observable again — follows
 * CS Launcher Plus, (C) CRAFT STUDIO, so that a world hosted from either launcher behaves the same.
 */
package com.movtery.zalithlauncher.hostagent;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.nio.charset.StandardCharsets;
import java.security.ProtectionDomain;

/**
 * Lifts the integrated server's LAN player limit and makes LAN publishing observable.
 *
 * <p><b>Why an agent and not a mod.</b> A mod has to be installed per version, per loader, and is
 * invisible to the 80% of players who run plain vanilla. A {@code -javaagent} is passed on the JVM
 * command line, so the same jar covers vanilla, Fabric, Forge and NeoForge without the player
 * installing anything — which matters because the whole point is that hosting a world for friends
 * should not require a modding tutorial first.
 *
 * <p><b>Why shape recognition and not mappings.</b> The obfuscated name of the integrated player list
 * changes every release, and the deobfuscated name differs between vanilla, intermediary and
 * official-mojmap loaders. Instead the agent looks for the only class that has a three-object
 * constructor pushing the literal {@code 8} into a four-argument super constructor <em>and</em>
 * declares a {@code (SocketAddress, GameProfile)} login check. That description has held true across
 * every release since the limit was introduced, and it is mapping-free by construction.
 *
 * <p>Two independent patches:
 * <ol>
 *   <li><b>Player limit</b> — replaces {@code BIPUSH 8} with
 *       {@code Integer.getInteger("cs.host.maxPlayers", 8)}, so the launcher sets the ceiling with a
 *       system property and the vanilla default survives when it does not.</li>
 *   <li><b>Publish hook (26.x)</b> — since 26.2 the Multiplayer Options screen opens the world to LAN
 *       without logging anything, so the launcher never learns the port. The 26.x client is
 *       unobfuscated and declares an int field {@code publishedPort} plus boolean methods
 *       {@code publishServer}/{@code unpublishServer}; the agent prints
 *       {@code [cs-host] published=true port=N} and {@code [cs-host] Stopping server publish} so the
 *       log tail can see the world open and close.</li>
 * </ol>
 *
 * <p>Anything that does not match is returned untransformed. A class the agent does not recognise is
 * left exactly as the JVM found it — a transformer that guesses corrupts the game, and there is no
 * recovery from inside a running client.
 */
public final class CsHostAgent {

    /** System property carrying the player ceiling; read by the patched constructor. */
    public static final String PROPERTY = "cs.host.maxPlayers";

    /** Printed before the published flag; the launcher greps for this exact prefix. */
    public static final String PUBLISH_LINE = "[cs-host] published=";

    /** Printed when the world stops being open to LAN. */
    public static final String UNPUBLISH_LINE = "[cs-host] Stopping server publish";

    /** Constant-pool marker of the login check, used to skip classes cheaply before parsing. */
    private static final byte[] LOGIN_NEEDLE = "java/net/SocketAddress".getBytes(StandardCharsets.US_ASCII);

    /** Constant-pool marker of the 26.x integrated server, likewise a cheap pre-filter. */
    private static final byte[] PUBLISH_NEEDLE = "publishedPort".getBytes(StandardCharsets.US_ASCII);

    private CsHostAgent() {}

    /**
     * Agent entry point, called by the JVM before {@code main}.
     *
     * Registered with {@code canRetransform = false}: the agent only needs classes as they are first
     * loaded, and opting out of retransformation keeps it from interfering with mod loaders that
     * retransform the same classes.
     */
    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String name, Class<?> redefined,
                                    ProtectionDomain pd, byte[] bytes) {
                try {
                    if (name == null || bytes == null) return null;
                    // Never touch the platform: a broken java.base class is an unbootable JVM, and
                    // none of them can be the integrated player list anyway.
                    if (name.startsWith("java/") || name.startsWith("sun/") || name.startsWith("jdk/")) return null;
                    if (name.startsWith("com/movtery/zalithlauncher/hostagent/")) return null;

                    // Bytecode scanning is not free and thousands of classes pass through here, so
                    // both patches are gated on a constant-pool substring search first.
                    if (contains(bytes, PUBLISH_NEEDLE)) {
                        byte[] published = patchPublish(name, bytes);
                        if (published != null) return published;
                    }
                    if (!contains(bytes, LOGIN_NEEDLE)) return null;
                    return patchPlayerLimit(name, bytes);
                } catch (Throwable t) {
                    // A failure to patch one class must never abort class loading: the game would
                    // crash with a NoClassDefFoundError pointing somewhere unrelated.
                    System.err.println("[cs-host-agent] skipped " + name + ": " + t);
                    return null;
                }
            }
        }, false);
        System.out.println("[cs-host-agent] ready, " + PROPERTY + "=" + System.getProperty(PROPERTY, "(unset: 8)"));
    }

    // -----------------------------------------------------------------------
    // Patch 1: the 8-player LAN limit
    // -----------------------------------------------------------------------

    /**
     * Replaces the hard-coded LAN limit in the integrated player list's constructor.
     *
     * @return the patched bytecode, or null when {@code bytes} is some other class
     */
    static byte[] patchPlayerLimit(String name, byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);

        // The login check is what distinguishes the player list from any other class that happens to
        // push an 8 into a super constructor.
        boolean loginCheck = false;
        for (MethodNode m : cn.methods) {
            if (m.desc.startsWith("(Ljava/net/SocketAddress;L") && (m.access & Opcodes.ACC_STATIC) == 0) {
                loginCheck = true;
                break;
            }
        }
        if (!loginCheck) return null;

        int patched = 0;
        for (MethodNode m : cn.methods) {
            if (!"<init>".equals(m.name)) continue;
            InsnList insns = m.instructions;
            for (AbstractInsnNode n = insns.getFirst(); n != null; n = n.getNext()) {
                if (n.getOpcode() != Opcodes.BIPUSH || ((IntInsnNode) n).operand != 8) continue;

                // The 8 has to be the last argument of the super(...) call, not an unrelated literal
                // that happens to precede some other invoke.
                AbstractInsnNode next = n.getNext();
                if (!(next instanceof MethodInsnNode)) continue;
                MethodInsnNode call = (MethodInsnNode) next;
                if (call.getOpcode() != Opcodes.INVOKESPECIAL || !"<init>".equals(call.name)) continue;
                if (!call.desc.endsWith("I)V")) continue;
                if (cn.name.equals(call.owner) || !call.owner.equals(cn.superName)) continue;
                if (countArgs(call.desc) != 4) continue;

                // Integer.getInteger(PROPERTY, 8).intValue() — same width as the BIPUSH it replaces,
                // and the trailing NOP keeps the instruction list well-formed for the frame walker.
                InsnList repl = new InsnList();
                repl.add(new LdcInsnNode(PROPERTY));
                repl.add(new IntInsnNode(Opcodes.BIPUSH, 8));
                repl.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "getInteger",
                        "(Ljava/lang/String;I)Ljava/lang/Integer;", false));
                repl.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/Integer", "intValue", "()I", false));
                repl.add(new InsnNode(Opcodes.NOP));

                AbstractInsnNode last = repl.getLast();
                insns.insertBefore(n, repl);
                insns.remove(n);
                n = last;
                patched++;
            }
        }
        if (patched == 0) return null;

        // COMPUTE_MAXS only: the patch adds no branches and no locals, so frames are untouched and
        // COMPUTE_FRAMES (which would need a class hierarchy the agent cannot see) is unnecessary.
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("[cs-host-agent] player limit lifted in " + name
                + " (" + PROPERTY + "=" + System.getProperty(PROPERTY, "8") + ")");
        return cw.toByteArray();
    }

    // -----------------------------------------------------------------------
    // Patch 2: 26.x publish observability
    // -----------------------------------------------------------------------

    /**
     * Prints a line when a 26.x client publishes or unpublishes its integrated server.
     *
     * <p>Up to 25.x "Open to LAN" logged {@code Started serving on <port>} and echoed
     * {@code Local game hosted on port <port>} in chat; the launcher tails that line to start the
     * tunnel. From 26.2 the port is only shown on screen, so without this hook the launcher has no way
     * to learn that a world became shareable.
     *
     * @return the patched bytecode, or null when {@code bytes} is not the integrated server
     */
    static byte[] patchPublish(String name, byte[] bytes) {
        ClassNode cn = new ClassNode();
        new ClassReader(bytes).accept(cn, 0);

        boolean portField = false;
        for (FieldNode f : cn.fields) {
            if ("publishedPort".equals(f.name) && "I".equals(f.desc)) portField = true;
        }
        if (!portField) return null;

        // The printed port comes from getPort(); without it the line would carry a stale field value.
        boolean hasGetPort = false;
        for (MethodNode m : cn.methods) {
            if ("getPort".equals(m.name) && "()I".equals(m.desc)) hasGetPort = true;
        }

        int patched = 0;
        for (MethodNode m : cn.methods) {
            if ((m.access & Opcodes.ACC_STATIC) != 0 || (m.access & Opcodes.ACC_ABSTRACT) != 0) continue;

            if ("publishServer".equals(m.name) && m.desc.endsWith(")Z")) {
                InsnList insns = m.instructions;
                for (AbstractInsnNode n = insns.getFirst(); n != null; n = n.getNext()) {
                    if (n.getOpcode() != Opcodes.IRETURN) continue;
                    // Stack on arrival is [Z]. Build PUBLISH_LINE + flag + " port=" + getPort() with
                    // straight-line code only: no branches means no new stack-map frames, which is
                    // what keeps COMPUTE_MAXS sufficient.
                    InsnList add = new InsnList();
                    add.add(new InsnNode(Opcodes.DUP));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/String", "valueOf",
                            "(Z)Ljava/lang/String;", false));
                    add.add(new LdcInsnNode(PUBLISH_LINE));
                    add.add(new InsnNode(Opcodes.SWAP));
                    add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat",
                            "(Ljava/lang/String;)Ljava/lang/String;", false));
                    add.add(new LdcInsnNode(" port="));
                    add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat",
                            "(Ljava/lang/String;)Ljava/lang/String;", false));
                    add.add(new VarInsnNode(Opcodes.ALOAD, 0));
                    add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, cn.name, "getPort", "()I", false));
                    add.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Integer", "toString",
                            "(I)Ljava/lang/String;", false));
                    add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/String", "concat",
                            "(Ljava/lang/String;)Ljava/lang/String;", false));
                    add.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out",
                            "Ljava/io/PrintStream;"));
                    add.add(new InsnNode(Opcodes.SWAP));
                    add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                            "(Ljava/lang/String;)V", false));
                    insns.insertBefore(n, add);
                    patched++;
                }
            } else if ("unpublishServer".equals(m.name) || "teardownPublishedState".equals(m.name)) {
                // Closing the world to multiplayer without leaving it must be visible too, or the
                // tunnel stays up advertising a port nothing is listening on.
                InsnList add = new InsnList();
                add.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out",
                        "Ljava/io/PrintStream;"));
                add.add(new LdcInsnNode(UNPUBLISH_LINE));
                add.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println",
                        "(Ljava/lang/String;)V", false));
                m.instructions.insert(add);
                patched++;
            }
        }

        if (patched == 0 || !hasGetPort) {
            if (patched > 0) {
                // Half a hook is worse than none: it would print a line with a wrong port, and the
                // launcher would tunnel a port the game is not listening on.
                System.out.println("[cs-host-agent] " + name + " has publishServer but no getPort(): left alone");
            }
            return null;
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        System.out.println("[cs-host-agent] publish hook installed in " + name);
        return cw.toByteArray();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Parameter count of a method descriptor; used to confirm the four-argument super call. */
    static int countArgs(String desc) {
        int n = 0;
        int i = 1;
        while (i < desc.length() && desc.charAt(i) != ')') {
            char c = desc.charAt(i);
            if (c == 'L') {
                i = desc.indexOf(';', i) + 1;
                n++;
            } else if (c == '[') {
                i++;
            } else {
                i++;
                n++;
            }
        }
        return n;
    }

    /** True when {@code needle} occurs anywhere in {@code hay} — a cheap constant-pool pre-filter. */
    static boolean contains(byte[] hay, byte[] needle) {
        if (needle.length == 0 || hay.length < needle.length) return false;
        outer:
        for (int i = 0; i <= hay.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (hay[i + j] != needle[j]) continue outer;
            }
            return true;
        }
        return false;
    }
}
