"""
WebSocket-based notification service.

Clients (the React frontend) connect to /ws/{client_id} and receive JSON
push events whenever a ticket changes state.  The WebSocketManager keeps
a registry of live connections and broadcasts to all connected clients.
"""
from __future__ import annotations

import asyncio
import json
import logging
from typing import Any

from fastapi import WebSocket

from src.shared.events import TicketAssignedEvent, TicketCreatedEvent, TicketResolvedEvent

log = logging.getLogger(__name__)


class WebSocketManager:
    def __init__(self) -> None:
        self._connections: dict[str, WebSocket] = {}

    async def connect(self, client_id: str, websocket: WebSocket) -> None:
        await websocket.accept()
        self._connections[client_id] = websocket
        log.info("WS client %s connected (%d total)", client_id, len(self._connections))

    def disconnect(self, client_id: str) -> None:
        self._connections.pop(client_id, None)
        log.info("WS client %s disconnected (%d remaining)", client_id, len(self._connections))

    async def broadcast(self, payload: dict[str, Any]) -> None:
        if not self._connections:
            return
        message = json.dumps(payload, default=str)
        dead: list[str] = []
        for client_id, ws in list(self._connections.items()):
            try:
                await ws.send_text(message)
            except Exception:
                dead.append(client_id)
        for client_id in dead:
            self.disconnect(client_id)


# Singleton shared across the process
ws_manager = WebSocketManager()


class NotificationService:
    def __init__(self, manager: WebSocketManager = ws_manager) -> None:
        self._manager = manager

    async def notify_ticket_created(self, event: TicketCreatedEvent) -> None:
        await self._manager.broadcast({
            "type": "TICKET_CREATED",
            "ticketId": event.ticket_id,
            "title": event.title,
            "priority": event.priority,
            "category": event.category,
        })

    async def notify_ticket_assigned(self, event: TicketAssignedEvent) -> None:
        await self._manager.broadcast({
            "type": "TICKET_ASSIGNED",
            "ticketId": event.ticket_id,
            "agentId": event.agent_id,
            "agentName": event.agent_name,
        })

    async def notify_ticket_resolved(self, event: TicketResolvedEvent) -> None:
        await self._manager.broadcast({
            "type": "TICKET_RESOLVED",
            "ticketId": event.ticket_id,
            "agentId": event.agent_id,
            "resolutionMinutes": event.resolution_minutes,
        })
