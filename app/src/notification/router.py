from fastapi import APIRouter, WebSocket, WebSocketDisconnect

from src.notification.service import ws_manager

router = APIRouter(tags=["websocket"])


@router.websocket("/ws/{client_id}")
async def websocket_endpoint(websocket: WebSocket, client_id: str) -> None:
    await ws_manager.connect(client_id, websocket)
    try:
        while True:
            # Keep connection alive; client may send pings
            await websocket.receive_text()
    except WebSocketDisconnect:
        ws_manager.disconnect(client_id)
