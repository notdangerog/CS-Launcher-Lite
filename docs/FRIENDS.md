# CS friends — accounts, presence, invites and chat

CS Launcher Lite talks to the same friends backend as **CS Launcher Plus**, so one account works in
both launchers and a world hosted from either one is joinable from the other.

```
ZalithLauncher/src/main/java/com/movtery/zalithlauncher/friends/
├── FriendsApi.kt      Supabase PostgREST transport (RPC calls, chat image uploads)
├── FriendsModels.kt   Wire-adjacent models: friend, request, invite, message, account, state
└── FriendsClient.kt   The one live link: presence, polling, session, actions
```

UI: `ui/screens/lite/CsFriendsScreen.kt`, reachable from the rail.

## The backend

| | |
| --- | --- |
| Project | `https://ygteavpkursyeyfthlxm.supabase.co` |
| Calls | `POST /rest/v1/rpc/<function>` |
| Auth | the public **anon** key in the `apikey` header, plus a per-player session token as `p_token` |
| Chat images | `POST /storage/v1/object/cs-chat/<name>` with `x-upsert`, read back from `/storage/v1/object/public/cs-chat/<name>` |

The anon key is public by design — Supabase exposes it to every client and row-level security does the
authorizing. **A session token is stored on the device; a password never is.** A stored token shorter
than 16 characters is treated as signed-out rather than sent and rejected.

There are no server-side secrets in this codebase. Admin keys and database credentials belong on the
server and are not committed here.

## Functions

| Function | Arguments | Returns |
| --- | --- | --- |
| `cs_sync` | `p_token`, `p_status`, `p_address`, `p_world`, `p_engine`, `p_since`, `p_version` | `me`, `friends`, `requests`, `invites`, `messages`, `now` |
| `cs_account_register` | `p_username`, `p_password`, `p_display` | session |
| `cs_account_login` | `p_username`, `p_password` | session |
| `cs_account_password` | `p_token`, `p_old`, `p_new` | — |
| `cs_add_friend` | `p_token`, `p_code` | — |
| `cs_respond` | `p_token`, `p_other`, `p_accept` | — |
| `cs_remove_friend` | `p_token`, `p_other` | — |
| `cs_send` | `p_token`, `p_to`, `p_text`, `p_reply_to?`, `p_image?` | — |
| `cs_invite` | `p_token`, `p_to`, `p_address`, `p_world`, `p_engine`, `p_version` | — |

`p_to` on `cs_invite` is **null to invite every friend**; the backend does the fan-out, so "invite all"
is one request rather than N.

A reply that is not a JSON object is wrapped under `"value"` so callers can always treat the result as
an object — PostgREST returns a bare scalar for some functions and a row for others, and a transport
layer that forces every call site to guess which is a transport layer with a bug in it.

## `cs_sync` is both the poll and the heartbeat

One round trip reports this player's presence *and* returns friends, requests, invites and messages.
There is no second request to keep in step with the first — a separate presence ping is exactly the
kind of thing that drifts and leaves a friend looking offline while they are mid-conversation.

Presence fields:

| Status | `p_address` | `p_world` | `p_engine` |
| --- | --- | --- | --- |
| `offline` | null | null | `launcher` |
| `online` | null | null | `launcher` |
| `playing` | null | optional | `java` |
| `hosting` | `name.e4mc.link` | world name | `java` |

### Poll rates follow attention, not the clock

| Situation | Interval | Why |
| --- | --- | --- |
| A conversation is open | 3 s | chat has to feel live |
| The friends surface is visible | 8 s | a list the player is reading |
| Backgrounded | 20 s | a heartbeat, not a conversation |
| After an error | 30 s | a dead network is not hammered |

The loop **stops entirely** when no screen is watching, no background watch is registered and no world
is being hosted — a signed-in player who never opens the friends screen costs no radio time. Hosting
keeps the loop alive on its own, because a published world must keep reporting presence or its guests
watch the host vanish while still playing in their world.

`p_since` is a **message id, not a timestamp**: the backend pages on it, so two messages sharing a
timestamp can neither be skipped nor replayed. Sending a timestamp would silently drop chat.

## Models

`CsFriend`, `CsFriendRequest`, `CsWorldInvite`, `CsChatMessage`, `CsFriendAccount`, `CsFriendsState`.

Ids are **strings**, because Supabase serialises bigints that way and a JavaScript-precision `Long`
round-trip through JSON is how an id quietly changes value. Ordering that needs numbers converts
explicitly and ignores what it cannot parse, rather than resetting the cursor and replaying history.

Derived state lives on the models, not in the UI:

- `CsFriend.isJoinable` — has an address, so a Join button is meaningful
- `CsWorldInvite.isLive(now)` — within the ten-minute window
- `CsChatMessage.mine` — **derived from the sender id, never trusted from the wire**; the backend does
  not echo it, and inferring it keeps both sides of a conversation consistent by construction
- `CsFriendsState.attentionCount` — incoming requests plus live invites, which is what a badge should
  count

`serverNow` comes from the backend, and invite expiry is judged against it: device clocks drift, and a
ten-minute invite measured against a wrong local clock either vanishes early or lingers for hours.

## Local-only decisions

Two things are deliberately **not** sent to the server:

- **Declining an invite.** It is a UI concern, and the invite expires by itself after ten minutes.
  Telling the backend would need a write path for a state that already resolves itself, and would make
  a mistap permanent. A local decline survives the next sync, so a dismissed invite does not reappear
  eight seconds later — which would read as the app ignoring the player.
- **Sending a message optimistically.** The message appears when the sync returns it. One source of
  truth beats a "sent" ghost that never arrived because the request failed.

## Lifecycle

`FriendsClient.initialize(context)` runs in `ZLApplication.onCreate`: it restores a stored session and,
when signed in, starts the loop. Presence therefore begins when the launcher opens, not when the player
happens to visit the friends screen — and it costs nothing at all when signed out.

`MainActivity` toggles `setBackgroundWatch` on resume/stop, which moves the loop between the visible
and background rates instead of stopping it: an invitation that arrives while the launcher is in a
pocket still lands.

Each visible surface calls `acquireWatcher()`, which returns a **release function** so a caller cannot
forget to give its watcher back — an unbalanced acquire would keep the radio warm forever.

## Errors

`FriendsApiException` carries the backend's own wording ("That code is already taken"), which is why
`messageFor()` tests it before its `IOException` supertype: the generic "no connection" branch would
otherwise swallow the one message that actually tells the player what to do differently.
