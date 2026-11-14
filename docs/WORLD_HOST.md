# World hosting — how CS Launcher Lite shares a world with friends

A Java Edition world opened to LAN is only reachable on the local network, and vanilla caps it at
**8 players**. This launcher lifts both limits so a player can host for friends who are not on the
same Wi‑Fi, with no port forwarding and no VPN.

```
Open to LAN  ──►  game log line  ──►  CsGameLogTap  ──►  WorldHostManager
                                                              │
                                            ┌─────────────────┼──────────────────┐
                                            ▼                 ▼                  ▼
                                    E4mcBridge (JNI)   HostSettings        FriendsClient
                                    tunnel + policy    limit/access        presence "hosting"
                                            │                                + invites
                                            ▼
                                    name.e4mc.link  ──►  friends tap Join  ──►  JoinWorld
```

## What is in the repository

| Path | Role |
| --- | --- |
| `HostAgent/` | Gradle module: the `-javaagent` that lifts the 8‑player limit and makes 26.x LAN publishing observable |
| `host/HostSettings.kt` | Player limit, access mode, relay, server icon, invites, kicked and "joined before" lists |
| `host/HostAgentInstaller.kt` | Extracts and verifies the agent jar, builds the `-javaagent` / `-Dcs.host.maxPlayers` arguments |
| `host/CsGameLogTap.kt` | Incremental tail of the game log |
| `host/WorldHostManager.kt` | The session: log line → tunnel → presence → guests → refusals |
| `host/E4mcBridge.kt` | Safe Kotlin façade over the tunnel JNI; never throws, never invents an address |
| `host/HostServerIcon.kt` | Writes the 64×64 `server-icon.png` guests see |
| `host/JoinWorld.kt` | Turns an invite into a launch: address normalization, TTL, cross‑version warning |
| `com/craftstudio/cslauncher/e4mc/E4mcNative.java` | JNI declaration set, kept at the FQN the prebuilt library exports |
| `ui/screens/lite/CsWorldHostScreen.kt` | The hosting screen |

## The two things the launcher cannot build for you

Both are optional. Without them the launcher still runs, still launches games, and reports hosting as
unavailable rather than crashing.

### 1. `libcs_e4mc.so` — the tunnel

The e4mc relay client, built by the CS Engine CI and used by CS Launcher Plus. It publishes a local
TCP port under a public `*.e4mc.link` address over QUIC.

Drop it here:

```
ZalithLauncher/src/main/jniLibs/arm64-v8a/libcs_e4mc.so
```

`E4mcNative` is declared in the package `com.craftstudio.cslauncher.e4mc` **on purpose**: JNI resolves
an unregistered native method from a symbol mangled out of the declaring class name
(`Java_com_craftstudio_cslauncher_e4mc_E4mcNative_nativeStart`), so keeping that FQN lets a library
built elsewhere link unchanged instead of being rebuilt against this launcher's package names.

`E4mcBridge.load()` reports why the tunnel is missing, and every other call swallows
`UnsatisfiedLinkError` — an older `.so` without `nativeGuestNames` or `nativeKick` degrades to a guest
count instead of failing.

### 2. Nothing else

The host agent is built from source by `:HostAgent` and copied into the app assets by the
`copyHostAgent` task, so it needs no manual step:

```bash
./gradlew :HostAgent:shadowJar          # produces HostAgent/build/libs/cs-host-agent.jar
./gradlew :ZalithLauncher:assembleDebug -Darch=arm64
```

The built jar is git-ignored; it is a build product, not a source file.

## How the 8‑player limit is lifted

Vanilla's integrated server builds its player list as
`new IntegratedPlayerList(server, registries, storage)`, whose constructor calls
`super(server, registries, storage, 8)`. That literal `8` is the LAN cap.

`CsHostAgent` replaces it with `Integer.getInteger("cs.host.maxPlayers", 8)`, so the launcher sets the
ceiling with a system property and the vanilla default survives when it does not.

The class is found **by shape, not by name**: the only class with a three-object constructor pushing
the literal `8` into a four-argument super constructor that also declares a
`(SocketAddress, GameProfile)` login check. That description is mapping-free, so one agent jar covers
obfuscated vanilla, Fabric intermediary and Forge/NeoForge official names alike — a mod would have to
be installed per version and per loader, and would not exist at all for the players who run vanilla.

Since 26.2 the Multiplayer Options screen opens a world to LAN **without logging anything**, so the
launcher would never learn the port. The agent's second patch hooks the unobfuscated 26.x
`publishedPort` field and `publishServer` / `unpublishServer` methods and prints:

```
[cs-host] published=true port=51837
[cs-host] Stopping server publish
```

Everything that does not match is returned untransformed. A transformer that guesses corrupts the
game, and there is no recovery from inside a running client.

ASM is **relocated** into `com.movtery.zalithlauncher.hostagent.shaded.asm` because a `-javaagent` jar
joins the system class path, where the game already ships its own ASM — Fabric Loader refuses to start
when it finds a second `org.objectweb.asm.ClassReader`. The `shadowJar` task fails the build if an
unrelocated ASM class ever leaks back in, and `HostAgentInstaller.verify()` refuses to hand the JVM a
jar that lost its `Premain-Class` or gained ASM under the original package.

## Who may join

Enforced **at the tunnel door**, not in the game: by the time the integrated server sees a connection
the player is already in, and it has no concept of an allow-list.

| Mode | Admitted |
| --- | --- |
| Anyone | Everyone with the address, up to the limit |
| Friends | The current CS friends list — re-read on every poll, so a friend added mid-game is let in without restarting the tunnel |
| Invited | Players invited this session, plus everyone in the "joined before" memory |

The invited-only memory is what makes an invite a one-time act instead of a per-session ritual: a
friend who joined last week can come back without being re-invited, the way a Bedrock Realm works.
`HostSettings.kick()` also forgets the player, so a kicked guest is not silently readmitted by that
memory.

A guest the list keeps out is refused before any Minecraft packet is exchanged and is shown
`HostSettings.closedReason`, so a refusal is never a mystery. Refusals surface on the hosting card:
without them a friend who cannot get in concludes the host is ignoring them, and the host has no other
way to learn the access list is wrong.

## Why the log is watched instead of the game

The game log is the one thing every version, mod loader and renderer already agrees on. The
"open to LAN" line has been printed there since long before any of them had an event bus, and it
survives obfuscation, remapping, and mods that replace the logger. `CsGameLogTap` costs one 250 ms
poll and works from 1.8 through 26.x — more than can be said for any in-process hook.

It matches every wording that has ever appeared, because which one shows up depends on the version:

```
Local game hosted on port N        (chat echo, ≤25.x)
Started serving on N               (log, ≤25.x)
Published LAN server on port N     (26.x)
open to LAN. The port number is N  (older still)
[cs-host] published=true port=N    (added by the agent, 26.x)
```

The tail starts at the current end of the file, so a previous session's port line cannot tunnel a port
nothing is listening on, and restarts from zero when the file is truncated.

## Presence and invites

When the relay assigns an address, `WorldHostManager` publishes it through
`FriendsClient.setHosting(address, world, engine = "java")`. Friends see the status `hosting` with the
address attached and can join straight from the list — the same backend CS Launcher Plus uses, so a
world hosted from either launcher looks identical to the guest.

`FriendsClient.invite(toId = null, …)` fans one request out to the whole friends list; offline friends
find the invite waiting next time they open the launcher. Invites live for **ten minutes**, which is
long enough to read a notification and short enough that a stale address cannot be tapped into a dead
world.

## Joining

`JoinWorld` normalizes the address (a bare `abc.e4mc.link` means port 25565; a pasted
`https://abc.e4mc.link/` from a chat app is cleaned), stores it with a three-minute TTL, and hands it
to the launch pipeline through `Event.Launch.PlayServer`.

It deliberately builds **no game arguments**. `LaunchArgs` already emits `--quickPlayMultiplayer
host:port` on 1.20+ and falls back to `--server host --port N` on anything older, choosing from the
version's own `VersionInfo`. A second implementation of "join" is how a launcher ends up with two
disagreeing ideas of what joining means, and the failure mode is a silently ignored command-line
argument.

Java Edition has no cross-version play — the server refuses the handshake — so
`JoinWorld.versionWarning()` compares the release numbers and warns **before** the launch instead of
letting the player wait through one to discover it in a disconnect screen.

## Failure behaviour

Nothing in the hosting path may fail a launch. A player who cannot host is still a player who wants to
play, so every step is best-effort:

- no tunnel library → the hosting screen says so, the world still opens to LAN locally
- agent jar missing or unverified → the world hosts at vanilla's 8 players
- relay unreachable → `FAILED` with a reason after a 45 s dial timeout, not an endless spinner
- server icon unwritable → the game's default icon
- guest names unsupported by an older `.so` → occupancy count instead

`shutdownForProcessExit()` is the one blocking call in the module. It runs from the native exit hook,
where there is no dispatcher left to resume a coroutine on, so it stops the tunnel and clears presence
with a bounded 1.5 s wait. Without it, friends keep seeing a world that no longer exists until their
next poll.
