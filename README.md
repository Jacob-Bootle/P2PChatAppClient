# LAN P2P Chat

A simple, multithreaded Java command-line chat app for people on the same local network. Connect directly to another peer or use the optional Python discovery server to find users.

## Features

- Direct peer-to-peer messaging over TCP.
- Multiple concurrent chats, with commands to switch between conversations.
- Incoming message notifications while another chat is selected or no chat is open.
- In-memory chat history, retained after disconnecting until the app exits.
- Optional user discovery through the [Python companion discovery server](https://github.com/Jacob-Bootle/P2PChatAppDiscoverServer).

## Screenshots

### Startup and command help

<p align="center">
  <a href="https://github.com/user-attachments/assets/3b1c136f-03bc-4bb9-a368-904c1f28c0e5">
    <img src="https://github.com/user-attachments/assets/3b1c136f-03bc-4bb9-a368-904c1f28c0e5" width="529" alt="Startup prompts for a name and listening port, followed by the available chat commands" />
  </a>
</p>

<table>
  <tr>
    <td width="50%" valign="top">
      <strong>Find peers through discovery</strong><br /><br />
      <a href="https://github.com/user-attachments/assets/c628d8f9-b834-4820-9767-19520043dbd2">
        <img src="https://github.com/user-attachments/assets/c628d8f9-b834-4820-9767-19520043dbd2" width="378" alt="Joining the discovery server and listing User2's IP address and listening port" />
      </a>
    </td>
    <td width="50%" valign="top">
      <strong>Open an incoming chat</strong><br /><br />
      <a href="https://github.com/user-attachments/assets/ea136c8f-65f4-4e97-821f-451d77b82229">
        <img src="https://github.com/user-attachments/assets/ea136c8f-65f4-4e97-821f-451d77b82229" width="380" alt="Receiving a chat from Jacob, opening it, and exchanging messages" />
      </a>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <strong>Connect and send messages</strong><br /><br />
      <a href="https://github.com/user-attachments/assets/54b67826-10ad-437b-b1eb-0aebcc0f7d43">
        <img src="https://github.com/user-attachments/assets/54b67826-10ad-437b-b1eb-0aebcc0f7d43" width="249" alt="Connecting directly to User2 and exchanging Hello messages" />
      </a>
    </td>
    <td width="50%" valign="top">
      <strong>Close the view and keep receiving</strong><br /><br />
      <a href="https://github.com/user-attachments/assets/a260dda4-1d40-4b09-9d6e-075173927638">
        <img src="https://github.com/user-attachments/assets/a260dda4-1d40-4b09-9d6e-075173927638" width="255" alt="Closing the selected chat view while continuing to receive notifications from User2" />
      </a>
    </td>
  </tr>
  <tr>
    <td width="50%" valign="top">
      <strong>Disconnect and retain history</strong><br /><br />
      <a href="https://github.com/user-attachments/assets/534b3c7f-4901-45e4-bd22-16d0f305f227">
        <img src="https://github.com/user-attachments/assets/534b3c7f-4901-45e4-bd22-16d0f305f227" width="334" alt="Opening User2's chat history before disconnecting the conversation" />
      </a>
    </td>
    <td width="50%" valign="top">
      <strong>See when a peer disconnects</strong><br /><br />
      <a href="https://github.com/user-attachments/assets/b7a1f8f0-fefe-432f-830e-3843c624f89d">
        <img src="https://github.com/user-attachments/assets/b7a1f8f0-fefe-432f-830e-3843c624f89d" width="268" alt="Conversation history with a system notification that Jacob closed the chat" />
      </a>
    </td>
  </tr>
</table>

Click any screenshot to view it at full size.

## Getting started

### Requirements

- **JDK 26**, matching the Java version configured in `pom.xml`.
- **Maven**, or IntelliJ IDEA with Maven support.
- Two app instances on the same LAN, or on one computer for local testing.

### Build and run

Clone this repository and open its directory:

```sh
git clone https://github.com/Jacob-Bootle/P2PChatAppClient.git
cd P2PChatAppClient
```

Build the app and copy its runtime dependencies:

```sh
mvn package dependency:copy-dependencies
```

Run on macOS or Linux:

```sh
java -cp "target/classes:target/dependency/*" Main
```

Run on Windows:

```powershell
java -cp "target/classes;target/dependency/*" Main
```

Alternatively, open the project in IntelliJ IDEA, select JDK 26, load the Maven dependencies, and run `Main` from `src/main/java/Main.java`.

### Start a direct chat

1. Start the app on both computers and choose distinct names without spaces, such as `User1` and `User2`.
2. On User2's computer, enter a listening port at startup, such as `52587`.
3. On User1's computer, connect using User2's LAN IP address and listening port:

   ```text
   /connect 192.168.1.20 52587
   Hello!
   ```

4. On User2's computer, select the incoming conversation and reply:

   ```text
   /open User1
   Hello!
   ```

Replace `192.168.1.20` with the peer's actual LAN address. For two instances on the same computer, use `127.0.0.1` and give each listener a different port.

You can leave the listening port blank to start with outgoing connections only, then add a listener using `/listen <port>`.

### Use the discovery server

Set up the [Python companion discovery server](https://github.com/Jacob-Bootle/P2PChatAppDiscoverServer) on a computer reachable by both peers. Each client must have a listening port before joining it.

For example, if the discovery server is running at `192.168.1.10` on port `22222`:

```text
/listen 52587
/discovery 192.168.1.10 22222
/list
```

If you already chose a listening port at startup, skip `/listen`. Use the IP address and port shown by `/list` with `/connect <host> <port>` to start a chat.

The discovery server helps peers find one another; chat messages travel directly between clients. Use the server's actual LAN address when connecting from another computer. `0.0.0.0` is a server bind address, not a destination address for remote clients.

## Commands

| Command | Description |
| --- | --- |
| `/listen <port>` | Start an additional listener for incoming chats. |
| `/connect <host> <port>` | Connect to a peer and select the conversation. |
| `/discovery <host> <port>` | Register with a discovery server using your listening address and port. |
| `/undiscover` | Disconnect from the discovery server. |
| `/list` | List other users registered with the discovery server. |
| `/chats` | List conversations, their connection status, and message counts. |
| `/open <name>` | Select an existing conversation and display its history. |
| `/history` | Display the selected conversation's history. |
| `/close` | Deselect the conversation while continuing to receive messages. |
| `/disconnect` | Disconnect the selected conversation and retain its history for this session. |
| `/help` | Show the command list. |
| `/quit` | Close all chats and listeners, leave discovery, and exit. |

Text that does not begin with `/` is sent to the selected chat. Incoming chats must be selected with `/open <name>` before replying.

## Network scope and limitations

- **Designed for a single LAN.** Cross-network chat is not currently implemented. Extending it would require reachable public endpoints with manual port forwarding, or a transport such as WebRTC data channels with signaling and NAT traversal.
- **Peer ports must be reachable.** Allow inbound TCP traffic on each chosen listening port. Guest Wi-Fi or client isolation may prevent devices from connecting even on the same network.
- **History is session-only.** Messages are held in memory and are lost when the app exits.
- **Disconnected chats cannot currently be reconnected under the same name within a session.** Restart the app to create a new conversation with that peer.
- **Traffic is unencrypted and peers are not authenticated.** The current implementation uses plain TCP for chats and `ws://` for discovery; use it on a trusted local network.
