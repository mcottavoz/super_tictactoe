const WebSocket = require('ws');
const PORT = process.env.PORT || 8080;
const wss  = new WebSocket.Server({ port: PORT });

// rooms : code -> { host: WebSocket, guest: WebSocket | null }
const rooms = new Map();

function generateCode() {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    let code;
    do {
        code = Array.from({ length: 4 }, () =>
            chars[Math.floor(Math.random() * chars.length)]
        ).join('');
    } while (rooms.has(code));
    return code;
}

function send(ws, obj) {
    if (ws && ws.readyState === WebSocket.OPEN)
        ws.send(JSON.stringify(obj));
}

wss.on('connection', (ws) => {

    ws.on('message', (data) => {
        let msg;
        try { msg = JSON.parse(data.toString()); }
        catch (_) { return; }

        switch (msg.type) {

            case 'create': {
                const code = generateCode();
                rooms.set(code, { host: ws, guest: null });
                ws.roomCode = code;
                ws.role     = 'host';
                send(ws, { type: 'created', room: code, player: 'X' });
                console.log(`[+] Room created: ${code}`);
                break;
            }

            case 'join': {
                const code = (msg.room || '').toUpperCase().trim();
                const room = rooms.get(code);
                if (!room)        { send(ws, { type: 'error', message: 'Partie introuvable' }); break; }
                if (room.guest)   { send(ws, { type: 'error', message: 'Partie déjà complète' }); break; }

                room.guest  = ws;
                ws.roomCode = code;
                ws.role     = 'guest';

                // Tell guest they're O, then tell both to start
                send(ws,        { type: 'joined', room: code, player: 'O' });
                send(room.host, { type: 'start' });
                send(ws,        { type: 'start' });
                console.log(`[>] Room ${code} started`);
                break;
            }

            case 'move': {
                const room = ws.roomCode ? rooms.get(ws.roomCode) : null;
                if (!room) break;
                const opponent = ws.role === 'host' ? room.guest : room.host;
                if (opponent) send(opponent, msg);
                break;
            }

            case 'ping': {
                send(ws, { type: 'pong' });
                break;
            }
        }
    });

    ws.on('close', () => {
        if (!ws.roomCode) return;
        const room = rooms.get(ws.roomCode);
        if (!room) return;
        const opponent = ws.role === 'host' ? room.guest : room.host;
        if (opponent) send(opponent, { type: 'disconnect' });
        rooms.delete(ws.roomCode);
        console.log(`[-] Room ${ws.roomCode} closed`);
    });

    ws.on('error', (e) => console.error('WS error:', e.message));
});

console.log(`Super Morpion server listening on port ${PORT}`);
