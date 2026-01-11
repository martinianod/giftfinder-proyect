// Mock data for development/demo mode
export const mockGiftRecommendations = {
  interpretedIntent: {
    recipient: "novia de 27 años",
    occasion: "aniversario",
    budget: 40000,
    interests: ["libros", "diseño"],
    currency: "ARS"
  },
  recommendations: [
    {
      id: "1",
      title: "Set de Libros de Diseño Gráfico",
      description: "Colección de 3 libros sobre diseño moderno, tipografía y color. Perfectos para diseñadores y entusiastas.",
      price: 28500,
      currency: "ARS",
      source: "MercadoLibre",
      url: "https://mercadolibre.com.ar",
      image: "https://images.unsplash.com/photo-1544716278-ca5e3f4abd8c?w=400"
    },
    {
      id: "2",
      title: "Lámpara de Escritorio Minimalista",
      description: "Lámpara LED moderna con diseño escandinavo. Luz regulable, perfecta para lectura.",
      price: 15900,
      currency: "ARS",
      source: "MercadoLibre",
      url: "https://mercadolibre.com.ar",
      image: "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=400"
    },
    {
      id: "3",
      title: "Kit de Marcadores Profesionales",
      description: "Set de 48 marcadores de colores para diseño y arte. Punta doble, colores vibrantes.",
      price: 12300,
      currency: "ARS",
      source: "MercadoLibre",
      url: "https://mercadolibre.com.ar",
      image: "https://images.unsplash.com/photo-1513542789411-b6a5d4f31634?w=400"
    },
    {
      id: "4",
      title: "Cuaderno de Bocetos Premium",
      description: "Cuaderno A4 con papel de alta calidad, tapa dura. Ideal para sketches y diseño.",
      price: 8500,
      currency: "ARS",
      source: "MercadoLibre",
      url: "https://mercadolibre.com.ar",
      image: "https://images.unsplash.com/photo-1531346878377-a5be20888e57?w=400"
    },
    {
      id: "5",
      title: "Curso Online de Diseño UX/UI",
      description: "Acceso a curso completo de diseño de interfaces. 40 horas de contenido.",
      price: 35000,
      currency: "ARS",
      source: "Udemy",
      url: "https://udemy.com",
      image: "https://images.unsplash.com/photo-1499750310107-5fef28a66643?w=400"
    },
    {
      id: "6",
      title: "Taza Térmica con Diseño Personalizado",
      description: "Taza de acero inoxidable que mantiene la temperatura. Diseño moderno y elegante.",
      price: 6800,
      currency: "ARS",
      source: "MercadoLibre",
      url: "https://mercadolibre.com.ar",
      image: "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=400"
    }
  ]
}

// Simulate API delay
export const delay = (ms) => new Promise(resolve => setTimeout(resolve, ms))
