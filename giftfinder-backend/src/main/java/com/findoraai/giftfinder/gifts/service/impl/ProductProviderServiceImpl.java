package com.findoraai.giftfinder.gifts.service.impl;

import com.findoraai.giftfinder.gifts.dto.GiftResponse;
import com.findoraai.giftfinder.gifts.dto.ParsedQuery;
import com.findoraai.giftfinder.gifts.service.ProductProviderService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductProviderServiceImpl implements ProductProviderService {

    @Override
    public List<GiftResponse> findProducts(ParsedQuery parsed) {

        return List.of(
                new GiftResponse(
                        "1",
                        "Libro de diseño minimalista",
                        "Un libro ideal para amantes del diseño y la estética.",
                        38000,
                        "ARS",
                        "https://via.placeholder.com/400?text=Libro+Diseno",
                        "https://tienda.com/product/1",
                        "Tienda Ejemplo",
                        4.8,
                        parsed.interests()
                ),
                new GiftResponse(
                        "2",
                        "Set de cuadernos ilustrados",
                        "Pack premium de cuadernos para diseño e ilustración.",
                        32000,
                        "ARS",
                        "https://via.placeholder.com/400?text=Cuadernos",
                        "https://tienda.com/product/2",
                        "Tienda Ejemplo",
                        4.6,
                        parsed.interests()
                )
        );
    }
}