package com.newspulse.ai.domain

data class CompanyMatch(
    val symbol: String,
    val companyName: String,
    val exchange: String = "NSE"
)

object CompanyRegistry {
    val REGISTRY: Map<String, CompanyMatch> = mapOf(
        // Heavyweights / Nifty 50
        "RELIANCE" to CompanyMatch("RELIANCE", "Reliance Industries"),
        "RELIANCE INDUSTRIES" to CompanyMatch("RELIANCE", "Reliance Industries"),
        "RIL" to CompanyMatch("RELIANCE", "Reliance Industries"),
        "JIO" to CompanyMatch("JIOFIN", "Jio Financial Services"),
        "JIO FINANCIAL" to CompanyMatch("JIOFIN", "Jio Financial Services"),
        "TCS" to CompanyMatch("TCS", "Tata Consultancy Services"),
        "TATA CONSULTANCY" to CompanyMatch("TCS", "Tata Consultancy Services"),
        "TATA MOTORS" to CompanyMatch("TATAMOTORS", "Tata Motors"),
        "TATAMOTORS" to CompanyMatch("TATAMOTORS", "Tata Motors"),
        "TATA STEEL" to CompanyMatch("TATASTEEL", "Tata Steel"),
        "TATA POWER" to CompanyMatch("TATAPOWER", "Tata Power"),
        "TATA CONSUMER" to CompanyMatch("TATACONSUM", "Tata Consumer Products"),
        "TATA CHEMICALS" to CompanyMatch("TATACHEM", "Tata Chemicals"),
        "TATA ELXSI" to CompanyMatch("TATAELXSI", "Tata Elxsi"),
        "TATA TECH" to CompanyMatch("TATATECH", "Tata Technologies"),
        "HDFC BANK" to CompanyMatch("HDFCBANK", "HDFC Bank"),
        "HDFCBANK" to CompanyMatch("HDFCBANK", "HDFC Bank"),
        "HDFC LIFE" to CompanyMatch("HDFCLIFE", "HDFC Life Insurance"),
        "HDFC AMC" to CompanyMatch("HDFCAMC", "HDFC Asset Management"),
        "INFOSYS" to CompanyMatch("INFY", "Infosys"),
        "INFY" to CompanyMatch("INFY", "Infosys"),
        "ICICI BANK" to CompanyMatch("ICICIBANK", "ICICI Bank"),
        "ICICIBANK" to CompanyMatch("ICICIBANK", "ICICI Bank"),
        "ICICI PRU" to CompanyMatch("ICICIPRULI", "ICICI Prudential Life"),
        "ICICI LOMBARD" to CompanyMatch("ICICIGI", "ICICI Lombard General"),
        "STATE BANK" to CompanyMatch("SBIN", "State Bank of India"),
        "SBI" to CompanyMatch("SBIN", "State Bank of India"),
        "SBIN" to CompanyMatch("SBIN", "State Bank of India"),
        "SBI LIFE" to CompanyMatch("SBILIFE", "SBI Life Insurance"),
        "SBI CARDS" to CompanyMatch("SBICARD", "SBI Cards & Payment"),
        "AXIS BANK" to CompanyMatch("AXISBANK", "Axis Bank"),
        "AXISBANK" to CompanyMatch("AXISBANK", "Axis Bank"),
        "KOTAK BANK" to CompanyMatch("KOTAKBANK", "Kotak Mahindra Bank"),
        "KOTAK MAHINDRA" to CompanyMatch("KOTAKBANK", "Kotak Mahindra Bank"),
        "INDUSIND" to CompanyMatch("INDUSINDBK", "IndusInd Bank"),
        "INDUSIND BANK" to CompanyMatch("INDUSINDBK", "IndusInd Bank"),
        "BANK OF BARODA" to CompanyMatch("BANKBARODA", "Bank of Baroda"),
        "PNB" to CompanyMatch("PNB", "Punjab National Bank"),
        "PUNJAB NATIONAL BANK" to CompanyMatch("PNB", "Punjab National Bank"),
        "CANARA BANK" to CompanyMatch("CANBK", "Canara Bank"),
        "YES BANK" to CompanyMatch("YESBANK", "Yes Bank"),
        "FEDERAL BANK" to CompanyMatch("FEDERALBNK", "Federal Bank"),
        "IDFC FIRST" to CompanyMatch("IDFCFIRSTB", "IDFC First Bank"),

        // Adani Group
        "ADANI ENTERPRISES" to CompanyMatch("ADANIENT", "Adani Enterprises"),
        "ADANIENT" to CompanyMatch("ADANIENT", "Adani Enterprises"),
        "ADANI PORTS" to CompanyMatch("ADANIPORTS", "Adani Ports & SEZ"),
        "ADANI POWER" to CompanyMatch("ADANIPOWER", "Adani Power"),
        "ADANI GREEN" to CompanyMatch("ADANIGREEN", "Adani Green Energy"),
        "ADANI TOTAL GAS" to CompanyMatch("ADANITOTAL", "Adani Total Gas"),
        "ADANI ENERGY" to CompanyMatch("ADANIENSOL", "Adani Energy Solutions"),
        "ADANI WILMAR" to CompanyMatch("AWL", "Adani Wilmar"),
        "NDTV" to CompanyMatch("NDTV", "New Delhi Television"),
        "AMBUJA" to CompanyMatch("AMBUJACEM", "Ambuja Cements"),
        "ACC" to CompanyMatch("ACC", "ACC Limited"),
        "ADANI" to CompanyMatch("ADANIENT", "Adani Enterprises"),

        // Auto & Consumer
        "MARUTI" to CompanyMatch("MARUTI", "Maruti Suzuki"),
        "MARUTI SUZUKI" to CompanyMatch("MARUTI", "Maruti Suzuki"),
        "MAHINDRA" to CompanyMatch("M&M", "Mahindra & Mahindra"),
        "M&M" to CompanyMatch("M&M", "Mahindra & Mahindra"),
        "BAJAJ AUTO" to CompanyMatch("BAJAJ-AUTO", "Bajaj Auto"),
        "HERO MOTOCORP" to CompanyMatch("HEROMOTOCO", "Hero MotoCorp"),
        "EICHER MOTORS" to CompanyMatch("EICHERMOT", "Eicher Motors (Royal Enfield)"),
        "ROYAL ENFIELD" to CompanyMatch("EICHERMOT", "Eicher Motors (Royal Enfield)"),
        "TVS MOTOR" to CompanyMatch("TVSMOTOR", "TVS Motor Company"),
        "BAJAJ FINANCE" to CompanyMatch("BAJFINANCE", "Bajaj Finance"),
        "BAJAJ FINSERV" to CompanyMatch("BAJAJFINSV", "Bajaj Finserv"),
        "HINDUSTAN UNILEVER" to CompanyMatch("HINDUNILVR", "Hindustan Unilever"),
        "HUL" to CompanyMatch("HINDUNILVR", "Hindustan Unilever"),
        "ITC" to CompanyMatch("ITC", "ITC Limited"),
        "NESTLE" to CompanyMatch("NESTLEIND", "Nestle India"),
        "BRITANNIA" to CompanyMatch("BRITANNIA", "Britannia Industries"),
        "DABUR" to CompanyMatch("DABUR", "Dabur India"),
        "GODREJ CONSUMER" to CompanyMatch("GODREJCP", "Godrej Consumer Products"),
        "MARICO" to CompanyMatch("MARICO", "Marico"),
        "VARUN BEVERAGES" to CompanyMatch("VBL", "Varun Beverages"),
        "TITAN" to CompanyMatch("TITAN", "Titan Company"),
        "ASIAN PAINTS" to CompanyMatch("ASIANPAINT", "Asian Paints"),
        "BERGER PAINTS" to CompanyMatch("BERGEPAINT", "Berger Paints"),
        "PIDILITE" to CompanyMatch("PIDILITIND", "Pidilite Industries (Fevicol)"),

        // Tech & Telecom
        "WIPRO" to CompanyMatch("WIPRO", "Wipro"),
        "HCL TECH" to CompanyMatch("HCLTECH", "HCL Technologies"),
        "HCL TECHNOLOGIES" to CompanyMatch("HCLTECH", "HCL Technologies"),
        "TECH MAHINDRA" to CompanyMatch("TECHM", "Tech Mahindra"),
        "LTIMINDTREE" to CompanyMatch("LTIM", "LTIMindtree"),
        "PERSISTENT" to CompanyMatch("PERSISTENT", "Persistent Systems"),
        "COFORGE" to CompanyMatch("COFORGE", "Coforge"),
        "MPHASIS" to CompanyMatch("MPHASIS", "Mphasis"),
        "BHARTI AIRTEL" to CompanyMatch("BHARTIARTL", "Bharti Airtel"),
        "AIRTEL" to CompanyMatch("BHARTIARTL", "Bharti Airtel"),
        "VODAFONE IDEA" to CompanyMatch("IDEA", "Vodafone Idea"),
        "INDUS TOWERS" to CompanyMatch("INDUSTOWER", "Indus Towers"),

        // New Age Tech
        "ZOMATO" to CompanyMatch("ZOMATO", "Zomato"),
        "SWIGGY" to CompanyMatch("SWIGGY", "Swiggy"),
        "PAYTM" to CompanyMatch("PAYTM", "One97 Communications (Paytm)"),
        "ONE97" to CompanyMatch("PAYTM", "One97 Communications (Paytm)"),
        "NYKAA" to CompanyMatch("NYKAA", "FSN E-Commerce (Nykaa)"),
        "POLICYBAZAAR" to CompanyMatch("POLICYBZR", "PB Fintech (Policybazaar)"),
        "DELHIVERY" to CompanyMatch("DELHIVERY", "Delhivery"),
        "OLA ELECTRIC" to CompanyMatch("OLAELEC", "Ola Electric Mobility"),

        // Pharma & Healthcare
        "SUN PHARMA" to CompanyMatch("SUNPHARMA", "Sun Pharmaceutical"),
        "DR REDDY" to CompanyMatch("DRREDDY", "Dr. Reddy's Laboratories"),
        "CIPLA" to CompanyMatch("CIPLA", "Cipla"),
        "DIVIS LAB" to CompanyMatch("DIVISLAB", "Divi's Laboratories"),
        "APOLLO HOSPITALS" to CompanyMatch("APOLLOHOSP", "Apollo Hospitals"),
        "MANKIND PHARMA" to CompanyMatch("MANKIND", "Mankind Pharma"),
        "LUPIN" to CompanyMatch("LUPIN", "Lupin"),
        "AUROBINDO" to CompanyMatch("AUROPHARMA", "Aurobindo Pharma"),
        "TORRENT PHARMA" to CompanyMatch("TORNTPHARM", "Torrent Pharmaceuticals"),

        // Metals, Energy & Infra
        "LARSEN & TOUBRO" to CompanyMatch("LT", "Larsen & Toubro"),
        "L&T" to CompanyMatch("LT", "Larsen & Toubro"),
        "TATA STEEL" to CompanyMatch("TATASTEEL", "Tata Steel"),
        "JSW STEEL" to CompanyMatch("JSWSTEEL", "JSW Steel"),
        "HINDALCO" to CompanyMatch("HINDALCO", "Hindalco Industries"),
        "VEDANTA" to CompanyMatch("VEDL", "Vedanta"),
        "JINDAL STEEL" to CompanyMatch("JINDALSTEL", "Jindal Steel & Power"),
        "SAIL" to CompanyMatch("SAIL", "Steel Authority of India"),
        "COAL INDIA" to CompanyMatch("COALINDIA", "Coal India"),
        "NTPC" to CompanyMatch("NTPC", "NTPC Limited"),
        "POWER GRID" to CompanyMatch("POWERGRID", "Power Grid Corporation"),
        "ONGC" to CompanyMatch("ONGC", "Oil and Natural Gas Corp"),
        "OIL INDIA" to CompanyMatch("OIL", "Oil India"),
        "BPCL" to CompanyMatch("BPCL", "Bharat Petroleum"),
        "IOC" to CompanyMatch("IOC", "Indian Oil Corporation"),
        "HPCL" to CompanyMatch("HPCL", "Hindustan Petroleum"),
        "GAIL" to CompanyMatch("GAIL", "GAIL India"),
        "ULTRATECH" to CompanyMatch("ULTRACEMCO", "UltraTech Cement"),
        "GRASIM" to CompanyMatch("GRASIM", "Grasim Industries"),
        "HAL" to CompanyMatch("HAL", "Hindustan Aeronautics"),
        "BEL" to CompanyMatch("BEL", "Bharat Electronics"),
        "BHEL" to CompanyMatch("BHEL", "Bharat Heavy Electricals"),
        "IRCTC" to CompanyMatch("IRCTC", "Indian Railway Catering & Tourism"),
        "IRFC" to CompanyMatch("IRFC", "Indian Railway Finance Corp"),
        "RVNL" to CompanyMatch("RVNL", "Rail Vikas Nigam"),
        "MAZDOCK" to CompanyMatch("MAZDOCK", "Mazagon Dock Shipbuilders")
    )

    private val wordPattern = Regex("[A-Z0-9&.]+")

    fun resolve(text: String): CompanyMatch? {
        val uppercaseText = text.uppercase().trim()
        if (uppercaseText.isBlank()) return null

        // Direct key lookup
        REGISTRY[uppercaseText]?.let { return it }

        // Token matching
        val tokens = wordPattern.findAll(uppercaseText).map { it.value }.toSet()

        var bestMatch: CompanyMatch? = null
        var bestTokenCount = 0

        for ((alias, match) in REGISTRY) {
            val aliasTokens = wordPattern.findAll(alias).map { it.value }.toSet()
            if (aliasTokens.isNotEmpty() && tokens.containsAll(aliasTokens)) {
                if (aliasTokens.size > bestTokenCount) {
                    bestTokenCount = aliasTokens.size
                    bestMatch = match
                }
            }
        }

        return bestMatch
    }
}
