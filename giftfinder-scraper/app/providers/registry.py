"""
Provider registry - manages available product providers.
"""

import logging
from typing import Dict, List

from app.config import get_settings
from app.providers.base import ProductProvider
from app.providers.reference import ReferenceProvider
from app.providers.scraping import ScrapingProvider

logger = logging.getLogger(__name__)
settings = get_settings()


class ProviderRegistry:
    """
    Registry for managing and discovering product providers.
    
    Loads enabled providers from configuration and provides
    access to them.
    """

    def __init__(self):
        """Initialize the provider registry."""
        self._providers: Dict[str, ProductProvider] = {}
        self._load_providers()

    def _load_providers(self) -> None:
        """
        Load enabled providers from configuration.
        
        Reads ENABLED_PROVIDERS from settings and instantiates
        the corresponding provider classes.
        """
        # Get enabled providers from config
        enabled = getattr(settings, "enabled_providers", "reference,scraping")
        if isinstance(enabled, str):
            enabled_list = [p.strip() for p in enabled.split(",") if p.strip()]
        else:
            enabled_list = enabled

        logger.info(f"Loading providers: {enabled_list}")

        # Available provider classes
        available_providers = {
            "reference": ReferenceProvider,
            "scraping": ScrapingProvider,
        }

        # Instantiate enabled providers
        for provider_name in enabled_list:
            provider_class = available_providers.get(provider_name)
            if provider_class:
                try:
                    provider = provider_class()
                    self._providers[provider.name] = provider
                    logger.info(f"✅ Loaded provider: {provider.name}")
                except Exception as e:
                    logger.error(f"❌ Failed to load provider {provider_name}: {e}")
            else:
                logger.warning(f"⚠️  Unknown provider: {provider_name}")

        if not self._providers:
            logger.warning("No providers loaded! Using reference provider as fallback.")
            self._providers["reference"] = ReferenceProvider()

    def get_provider(self, name: str) -> ProductProvider:
        """
        Get a provider by name.
        
        Args:
            name: Provider name
            
        Returns:
            ProductProvider instance
            
        Raises:
            KeyError: If provider not found
        """
        return self._providers[name]

    def get_all_providers(self) -> List[ProductProvider]:
        """
        Get all loaded providers.
        
        Returns:
            List of ProductProvider instances
        """
        return list(self._providers.values())

    def has_provider(self, name: str) -> bool:
        """
        Check if a provider is loaded.
        
        Args:
            name: Provider name
            
        Returns:
            True if provider is loaded, False otherwise
        """
        return name in self._providers

    def get_provider_names(self) -> List[str]:
        """
        Get names of all loaded providers.
        
        Returns:
            List of provider names
        """
        return list(self._providers.keys())


# Global registry instance
_registry: ProviderRegistry = None


def get_registry() -> ProviderRegistry:
    """
    Get the global provider registry instance (singleton).
    
    Returns:
        ProviderRegistry instance
    """
    global _registry
    if _registry is None:
        _registry = ProviderRegistry()
    return _registry
