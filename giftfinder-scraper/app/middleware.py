"""
Middleware for request ID tracking, timing, and logging.
"""
import time
import uuid
from typing import Callable
from fastapi import Request, Response
from starlette.middleware.base import BaseHTTPMiddleware
from app.logging_config import set_request_id, clear_request_id, get_logger

logger = get_logger(__name__)


class RequestIdMiddleware(BaseHTTPMiddleware):
    """
    Middleware that:
    - Generates or extracts request IDs
    - Tracks request timing
    - Logs all requests and responses
    - Adds X-Request-ID header to responses
    """
    
    async def dispatch(self, request: Request, call_next: Callable) -> Response:
        # Generate or extract request ID
        request_id = request.headers.get('X-Request-ID', str(uuid.uuid4()))
        set_request_id(request_id)
        
        # Record start time
        start_time = time.time()
        
        # Log incoming request
        logger.info(
            "Incoming request",
            extra={
                'method': request.method,
                'path': request.url.path,
                'client_ip': request.client.host if request.client else None,
                'user_agent': request.headers.get('user-agent'),
            }
        )
        
        try:
            # Process request
            response = await call_next(request)
            
            # Calculate duration
            duration_ms = (time.time() - start_time) * 1000
            
            # Log response
            logger.info(
                "Request completed",
                extra={
                    'method': request.method,
                    'path': request.url.path,
                    'status_code': response.status_code,
                    'duration_ms': round(duration_ms, 2),
                }
            )
            
            # Add request ID to response headers
            response.headers['X-Request-ID'] = request_id
            
            return response
            
        except Exception as e:
            # Calculate duration
            duration_ms = (time.time() - start_time) * 1000
            
            # Log error
            logger.error(
                "Request failed",
                extra={
                    'method': request.method,
                    'path': request.url.path,
                    'duration_ms': round(duration_ms, 2),
                    'error': str(e),
                    'error_type': type(e).__name__,
                },
                exc_info=True
            )
            
            # Re-raise to let FastAPI handle it
            raise
            
        finally:
            # Clear request ID from context
            clear_request_id()
