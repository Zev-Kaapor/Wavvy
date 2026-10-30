package com.wavvy.app.core.radio

// The capitals of the places of the radios, which the site leaves out of the main cities of a page when they are not big enough for its list
// A country is found by its name as the site writes it, a state of Brazil by its name and its two letters
object RadioCapitals {
    // The capital of each state of Brazil, by the two letters the site puts after its name
    private val Brazil = mapOf(
        "AC" to ("Acre" to "Rio Branco"), "AL" to ("Alagoas" to "Maceió"), "AP" to ("Amapá" to "Macapá"), "AM" to ("Amazonas" to "Manaus"),
        "BA" to ("Bahia" to "Salvador"), "CE" to ("Ceará" to "Fortaleza"), "DF" to ("Distrito Federal" to "Brasília"), "ES" to ("Espírito Santo" to "Vitória"),
        "GO" to ("Goiás" to "Goiânia"), "MA" to ("Maranhão" to "São Luís"), "MT" to ("Mato Grosso" to "Cuiabá"), "MS" to ("Mato Grosso do Sul" to "Campo Grande"),
        "MG" to ("Minas Gerais" to "Belo Horizonte"), "PA" to ("Pará" to "Belém"), "PB" to ("Paraíba" to "João Pessoa"), "PR" to ("Paraná" to "Curitiba"),
        "PE" to ("Pernambuco" to "Recife"), "PI" to ("Piauí" to "Teresina"), "RJ" to ("Rio de Janeiro" to "Rio de Janeiro"), "RN" to ("Rio Grande do Norte" to "Natal"),
        "RS" to ("Rio Grande do Sul" to "Porto Alegre"), "RO" to ("Rondônia" to "Porto Velho"), "RR" to ("Roraima" to "Boa Vista"), "SC" to ("Santa Catarina" to "Florianópolis"),
        "SP" to ("São Paulo" to "São Paulo"), "SE" to ("Sergipe" to "Aracaju"), "TO" to ("Tocantins" to "Palmas")
    )

    // The capital of each country, by its name in Portuguese
    private val Countries = mapOf(
        "Afeganistão" to "Cabul", "África do Sul" to "Pretória", "Albânia" to "Tirana", "Alemanha" to "Berlim", "Andorra" to "Andorra-a-Velha", "Angola" to "Luanda",
        "Antígua e Barbuda" to "Saint John's", "Arábia Saudita" to "Riade", "Argélia" to "Argel", "Argentina" to "Buenos Aires", "Armênia" to "Erevan", "Austrália" to "Camberra",
        "Áustria" to "Viena", "Azerbaijão" to "Baku", "Bahamas" to "Nassau", "Bangladesh" to "Daca", "Barbados" to "Bridgetown", "Barein" to "Manama", "Bélgica" to "Bruxelas",
        "Belize" to "Belmopan", "Benin" to "Porto Novo", "Bielorrússia" to "Minsk", "Bolívia" to "La Paz", "Bósnia e Herzegovina" to "Sarajevo", "Botsuana" to "Gaborone",
        "Brasil" to "Brasília", "Brunei" to "Bandar Seri Begawan", "Bulgária" to "Sófia", "Burkina Faso" to "Uagadugu", "Burundi" to "Gitega", "Butão" to "Timbu",
        "Cabo Verde" to "Praia", "Camarões" to "Iaundé", "Camboja" to "Phnom Penh", "Canadá" to "Ottawa", "Catar" to "Doha", "Cazaquistão" to "Astana", "Chade" to "Ndjamena",
        "Chile" to "Santiago", "China" to "Pequim", "Chipre" to "Nicósia", "Colômbia" to "Bogotá", "Comores" to "Moroni", "Congo" to "Brazzaville",
        "Coreia do Norte" to "Pyongyang", "Coreia do Sul" to "Seul", "Costa do Marfim" to "Yamoussoukro", "Costa Rica" to "San José", "Croácia" to "Zagreb", "Cuba" to "Havana",
        "Dinamarca" to "Copenhague", "Djibuti" to "Djibuti", "Dominica" to "Roseau", "Egito" to "Cairo", "El Salvador" to "San Salvador", "Emirados Árabes Unidos" to "Abu Dhabi",
        "Equador" to "Quito", "Eritreia" to "Asmara", "Eslováquia" to "Bratislava", "Eslovênia" to "Liubliana", "Espanha" to "Madri", "Estados Unidos" to "Washington",
        "Estônia" to "Talin", "Eswatini" to "Mbabane", "Etiópia" to "Adis Abeba", "Fiji" to "Suva", "Filipinas" to "Manila", "Finlândia" to "Helsinque", "França" to "Paris",
        "Gabão" to "Libreville", "Gâmbia" to "Banjul", "Gana" to "Acra", "Geórgia" to "Tbilisi", "Granada" to "Saint George's", "Grécia" to "Atenas", "Guatemala" to "Cidade da Guatemala",
        "Guiana" to "Georgetown", "Guiné" to "Conacri", "Guiné Equatorial" to "Malabo", "Guiné-Bissau" to "Bissau", "Haiti" to "Porto Príncipe", "Holanda" to "Amsterdã",
        "Honduras" to "Tegucigalpa", "Hungria" to "Budapeste", "Iêmen" to "Sanaa", "Ilhas Marshall" to "Majuro", "Ilhas Salomão" to "Honiara", "Índia" to "Nova Délhi",
        "Indonésia" to "Jacarta", "Irã" to "Teerã", "Iraque" to "Bagdá", "Irlanda" to "Dublin", "Islândia" to "Reykjavik", "Israel" to "Jerusalém", "Itália" to "Roma",
        "Jamaica" to "Kingston", "Japão" to "Tóquio", "Jordânia" to "Amã", "Kiribati" to "Tarawa", "Kosovo" to "Pristina", "Kuwait" to "Cidade do Kuwait", "Laos" to "Vientiane",
        "Lesoto" to "Maseru", "Letônia" to "Riga", "Líbano" to "Beirute", "Libéria" to "Monróvia", "Líbia" to "Trípoli", "Liechtenstein" to "Vaduz", "Lituânia" to "Vilnius",
        "Luxemburgo" to "Luxemburgo", "Macedônia do Norte" to "Escópia", "Madagascar" to "Antananarivo", "Malásia" to "Kuala Lumpur", "Malaui" to "Lilongue", "Maldivas" to "Malé",
        "Mali" to "Bamaco", "Malta" to "Valeta", "Marrocos" to "Rabat", "Maurício" to "Port Louis", "Mauritânia" to "Nuakchott", "México" to "Cidade do México",
        "Micronésia" to "Palikir", "Moçambique" to "Maputo", "Moldávia" to "Chisinau", "Mônaco" to "Mônaco", "Mongólia" to "Ulan Bator", "Montenegro" to "Podgorica",
        "Myanmar" to "Naypyidaw", "Namíbia" to "Windhoek", "Nauru" to "Yaren", "Nepal" to "Katmandu", "Nicarágua" to "Manágua", "Níger" to "Niamei", "Nigéria" to "Abuja",
        "Noruega" to "Oslo", "Nova Zelândia" to "Wellington", "Omã" to "Mascate", "Países Baixos" to "Amsterdã", "Palau" to "Ngerulmud", "Panamá" to "Cidade do Panamá",
        "Papua-Nova Guiné" to "Port Moresby", "Paquistão" to "Islamabad", "Paraguai" to "Assunção", "Peru" to "Lima", "Polônia" to "Varsóvia", "Portugal" to "Lisboa",
        "Quênia" to "Nairóbi", "Quirguistão" to "Bisqueque", "Reino Unido" to "Londres", "República Centro-Africana" to "Bangui", "República Dominicana" to "Santo Domingo",
        "República Tcheca" to "Praga", "Romênia" to "Bucareste", "Ruanda" to "Kigali", "Rússia" to "Moscou", "Samoa" to "Apia", "San Marino" to "San Marino",
        "Santa Lúcia" to "Castries", "São Cristóvão e Névis" to "Basseterre", "São Tomé e Príncipe" to "São Tomé", "São Vicente e Granadinas" to "Kingstown", "Senegal" to "Dacar",
        "Serra Leoa" to "Freetown", "Sérvia" to "Belgrado", "Singapura" to "Singapura", "Síria" to "Damasco", "Somália" to "Mogadíscio", "Sri Lanka" to "Colombo",
        "Sudão" to "Cartum", "Sudão do Sul" to "Juba", "Suécia" to "Estocolmo", "Suíça" to "Berna", "Suriname" to "Paramaribo", "Tadjiquistão" to "Duchambe", "Tailândia" to "Bangkok",
        "Taiwan" to "Taipé", "Tanzânia" to "Dodoma", "Timor-Leste" to "Díli", "Togo" to "Lomé", "Tonga" to "Nuku'alofa", "Trinidad e Tobago" to "Port of Spain", "Tunísia" to "Túnis",
        "Turcomenistão" to "Asgabate", "Turquia" to "Ancara", "Tuvalu" to "Funafuti", "Ucrânia" to "Kiev", "Uganda" to "Kampala", "Uruguai" to "Montevidéu", "Uzbequistão" to "Tasquente",
        "Vanuatu" to "Port Vila", "Vaticano" to "Vaticano", "Venezuela" to "Caracas", "Vietnã" to "Hanói", "Zâmbia" to "Lusaca", "Zimbábue" to "Harare"
    )

    private val ByCountry: Map<String, String> = Countries.mapKeys { it.key.plain() }
    private val BrazilByUf: Map<String, Pair<String, String>> = Brazil
    private val StateTitle = Regex("^(.*?)\\s*\\(([A-Za-z]{2})\\)\\s*$")

    // The capital of a country, none when the name is not one the list knows
    fun ofCountry(name: String): String? = ByCountry[name.plain()]

    // True when the country is Brazil, which has the capitals of its states listed too
    fun isBrazil(name: String): Boolean = name.plain() == "brasil"

    // The capitals of the states of Brazil
    fun brazilStates(): List<String> = Brazil.values.map { it.second }.distinct()

    // The capital of a state of Brazil from its title on the site, such as Sergipe (SE), both the name and the letters have to be the ones of the same state
    fun ofBrazilState(title: String): String? {
        val match = StateTitle.matchEntire(title.trim()) ?: return null
        val state = BrazilByUf[match.groupValues[2].uppercase()] ?: return null
        return state.second.takeIf { state.first.plain() == match.groupValues[1].plain() }
    }
}
