package com.atlas.util

object Constants {

    const val FIRMS_MAP_KEY = "423cb2034992266a41ce2346b0c2389f"


    const val OPENWEATHER_API_KEY = "0778e9912655399ac59bf439fd9b4dbd"

    const val MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoidGhpYWdvcGVyeiIsImEiOiJjbXEzam1weHQwOW9zMnNvZm12cXhyMzlmIn0.dmiPJ-ciz3ZbUQOAp8mlvw"


    const val FIRMS_BASE_URL       = "https://firms.modaps.eosdis.nasa.gov/"
    const val WEATHER_BASE_URL     = "https://api.openweathermap.org/"

    const val FIRMS_AREA = "VIIRS_SNPP_NRT/-47,-24,-46,-23/1"

    const val RAIN_THRESHOLD_MM = 5.0


    const val RISK_RADIUS_METERS = 600.0

    val FLOOD_RISK_POINTS = listOf(
        Triple(-23.5209, -46.6350, "Marginal Tietê"),
        Triple(-23.5430, -46.6890, "Marginal Tietê — Lapa"),
        Triple(-23.5960, -46.7050, "Marginal Pinheiros"),
        Triple(-23.6100, -46.7200, "Marginal Pinheiros — Morumbi"),
        Triple(-23.5543, -46.6329, "Avenida do Estado"),
        Triple(-23.5470, -46.5170, "Avenida Aricanduva"),
        Triple(-23.5490, -46.5380, "Avenida Salim Farah Maluf"),
        Triple(-23.5190, -46.6290, "Avenida Santos Dumont"),
        Triple(-23.5150, -46.6270, "Avenida Cruzeiro do Sul"),
        Triple(-23.4750, -46.6820, "Avenida Inajar de Souza"),
        Triple(-23.5820, -46.5530, "Avenida Professor Luiz Ignácio Anhaia Mello"),
        Triple(-23.6780, -46.5210, "Avenida Guido Aliberti"),
        Triple(-23.6550, -46.5300, "Avenida dos Estados"),
        Triple(-23.5100, -46.6100, "Avenida Capitão João"),
        Triple(-23.6100, -46.7500, "Rodovia Raposo Tavares"),
        Triple(-23.7200, -46.7000, "Rodovia Régis Bittencourt"),
        Triple(-23.5580, -46.7300, "Avenida Escola Politécnica"),
        Triple(-23.6183, -46.6547, "Avenida Santo Amaro"),
        Triple(-23.5981, -46.6897, "Avenida das Nações Unidas"),
        Triple(-23.5512, -46.5785, "Avenida Alcântara Machado (Radial Leste)"),
        Triple(-23.5425, -46.5870, "Avenida Celso Garcia"),
        Triple(-23.6298, -46.6418, "Avenida Professor Abraão de Morais"),
        Triple(-23.6142, -46.6398, "Avenida Ricardo Jafet"),
        Triple(-23.5241, -46.6953, "Avenida Marquês de São Vicente"),
        Triple(-23.5683, -46.6558, "Avenida Nove de Julho"),
        Triple(-23.5789, -46.6423, "Avenida 23 de Maio"),
        Triple(-23.5361, -46.4347, "Avenida Jacu-Pêssego"),
        Triple(-23.5483, -46.5018, "Avenida Conde de Frontin"),
        Triple(-23.6412, -46.7001, "Avenida Roque Petroni Júnior"),
        Triple(-23.6354, -46.6563, "Avenida dos Bandeirantes"),
        Triple(-23.6189, -46.6892, "Avenida Jornalista Roberto Marinho"),
        Triple(-23.5671, -46.7425, "Avenida Francisco Morato")
    )
}
