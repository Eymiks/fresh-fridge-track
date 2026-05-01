package com.freshtrack.domain.catalog

data class ProductSubcategory(val key: String, val label: String, val keywords: List<String>)

data class ProductCategory(
    val key: String,
    val label: String,
    val icon: String,
    val keywords: List<String>,
    val subcategories: List<ProductSubcategory>
)

val PRODUCT_CATEGORIES = listOf(
    ProductCategory("all", "Tous", "LayoutGrid", emptyList(), emptyList()),
    ProductCategory(
        "fruits_legumes", "Fruits & Légumes", "Apple",
        listOf("fruit","légume","legume","salade","tomate","pomme","banane","carotte","courgette","poivron","oignon","ail","avocat","citron","orange","fraise","raisin","melon","pastèque","poire","cerise","abricot","pêche","mangue","ananas","kiwi","clémentine","mandarine","épinard","brocoli","chou","haricot vert","petit pois","concombre","aubergine","radis","navet","betterave","artichaut","asperge","endive","fenouil","poireau","céleri","champignon","vegetable","légumes","fruits"),
        listOf(
            ProductSubcategory("agrumes","Agrumes",listOf("citron","orange","clémentine","mandarine","pamplemousse","lime")),
            ProductSubcategory("herbes","Herbes",listOf("basilic","persil","ciboulette","menthe","thym","romarin","coriandre","aneth","estragon","sauge","origan")),
            ProductSubcategory("fruits","Fruits",listOf("fruit","pomme","banane","fraise","raisin","melon","pastèque","poire","cerise","abricot","pêche","mangue","ananas","kiwi","framboise","myrtille","figue","prune")),
            ProductSubcategory("legumes","Légumes",listOf("légume","legume","tomate","carotte","courgette","poivron","oignon","ail","avocat","épinard","brocoli","chou","haricot","petit pois","concombre","aubergine","radis","navet","betterave","artichaut","asperge","endive","fenouil","poireau","céleri","champignon","salade","vegetable"))
        )
    ),
    ProductCategory(
        "laitiers", "Laitiers", "Milk",
        listOf("lait","yaourt","yogourt","fromage","crème","beurre","dairy","cheese","cream","milk","mozzarella","emmental","gruyère","camembert","comté","roquefort","chèvre","ricotta","mascarpone","feta","parmesan","raclette","reblochon","skyr","kéfir","petit-suisse","faisselle","laitier","laitiers","crème fraîche"),
        listOf(
            ProductSubcategory("fromages","Fromages",listOf("fromage","cheese","mozzarella","emmental","gruyère","camembert","comté","roquefort","chèvre","ricotta","mascarpone","feta","parmesan","raclette","reblochon")),
            ProductSubcategory("yaourts","Yaourts",listOf("yaourt","yogourt","yogurt","skyr","kéfir","petit-suisse","faisselle")),
            ProductSubcategory("beurres","Beurres",listOf("beurre","butter")),
            ProductSubcategory("cremes","Crèmes",listOf("crème","cream","crème fraîche")),
            ProductSubcategory("laits","Laits",listOf("lait","milk"))
        )
    ),
    ProductCategory(
        "viandes", "Viandes", "Beef",
        listOf("viande","poulet","porc","bœuf","boeuf","veau","agneau","dinde","canard","lapin","steak","saucisse","jambon","lard","bacon","merguez","chipolata","côtelette","rôti","escalope","meat","chicken","beef","pork","charcuterie","pâté","rillettes","terrine","boudin","andouillette"),
        listOf(
            ProductSubcategory("volaille","Volaille",listOf("poulet","dinde","canard","pintade","chicken","volaille")),
            ProductSubcategory("boeuf","Bœuf",listOf("bœuf","boeuf","beef","veau","steak","rôti")),
            ProductSubcategory("porc","Porc",listOf("porc","pork","jambon","lard","bacon","côtelette")),
            ProductSubcategory("charcuterie","Charcuterie",listOf("charcuterie","saucisse","merguez","chipolata","pâté","rillettes","terrine","boudin","andouillette","saucisson","chorizo"))
        )
    ),
    ProductCategory(
        "poissons", "Poissons", "Fish",
        listOf("poisson","saumon","thon","cabillaud","crevette","moule","huître","sardine","maquereau","truite","sole","bar","dorade","merlu","colin","lieu","fish","seafood","crustacé","fruit de mer","calamar","poulpe","homard","langouste","crabe","surimi"),
        listOf(
            ProductSubcategory("crustaces","Crustacés",listOf("crevette","crustacé","homard","langouste","crabe","langoustine","gambas")),
            ProductSubcategory("fumes","Fumés",listOf("fumé","smoked","saumon fumé","truite fumée","haddock")),
            ProductSubcategory("poissons","Poissons",listOf("poisson","saumon","thon","cabillaud","sardine","maquereau","truite","sole","bar","dorade","merlu","colin","lieu","fish"))
        )
    ),
    ProductCategory(
        "boulangerie", "Boulangerie", "Croissant",
        listOf("pain","baguette","croissant","brioche","viennoiserie","pain de mie","toast","muffin","bagel","focaccia","ciabatta","pain complet","pain aux céréales","fougasse","naan","pita","wrap","tortilla","crêpe","gaufre","bakery","bread"),
        listOf(
            ProductSubcategory("viennoiseries","Viennoiseries",listOf("croissant","brioche","viennoiserie","pain au chocolat","chausson","muffin")),
            ProductSubcategory("pains","Pains",listOf("pain","baguette","bagel","focaccia","ciabatta","fougasse","naan","pita","wrap","tortilla","bread"))
        )
    ),
    ProductCategory("oeufs","Œufs","Egg",listOf("oeuf","œuf","oeufs","œufs","egg","eggs"),emptyList()),
    ProductCategory(
        "condiments","Condiments & Sauces","Droplets",
        listOf("sauce","ketchup","mayonnaise","moutarde","vinaigre","huile","vinaigrette","soja","tabasco","sriracha","pesto","harissa","curry","épice","épices","poivre","paprika","cumin","curcuma","cannelle","herbes","basilic","thym","romarin","origan","condiment","assaisonnement"),
        listOf(
            ProductSubcategory("huiles","Huiles",listOf("huile","oil")),
            ProductSubcategory("vinaigres","Vinaigres",listOf("vinaigre","vinegar")),
            ProductSubcategory("epices","Épices",listOf("épice","épices","poivre","paprika","cumin","curcuma","cannelle","sel","spice")),
            ProductSubcategory("sauces","Sauces",listOf("sauce","ketchup","mayonnaise","moutarde","soja","tabasco","sriracha","pesto","harissa","vinaigrette"))
        )
    ),
    ProductCategory(
        "boissons","Boissons","CupSoda",
        listOf("boisson","jus","eau","soda","coca","limonade","thé","café","bière","vin","alcool","sirop","smoothie","nectar","drink","beverage","lait de","energy","ice tea","infusion","tisane","chocolat chaud"),
        listOf(
            ProductSubcategory("jus","Jus",listOf("jus","juice","nectar","smoothie")),
            ProductSubcategory("sodas","Sodas",listOf("soda","coca","limonade","pepsi","sprite","fanta","energy","ice tea")),
            ProductSubcategory("cafes","Cafés",listOf("café","coffee","thé","tea","infusion","tisane","chocolat chaud")),
            ProductSubcategory("alcools","Alcools",listOf("bière","vin","alcool","beer","wine","whisky","vodka","rhum","gin","champagne"))
        )
    ),
    ProductCategory(
        "surgeles","Surgelés","Snowflake",
        listOf("surgelé","surgelés","glace","frozen","glacé","sorbet","congelé","congélation","pizza surgelée","frites","légumes surgelés"),
        listOf(
            ProductSubcategory("glaces","Glaces",listOf("glace","glacé","sorbet","ice cream")),
            ProductSubcategory("legumes","Légumes",listOf("légumes surgelés","frites","épinards surgelés")),
            ProductSubcategory("plats","Plats",listOf("pizza surgelée","plat surgelé","lasagne surgelée"))
        )
    ),
    ProductCategory(
        "plats_prepares","Plats préparés","UtensilsCrossed",
        listOf("plat préparé","plat cuisiné","barquette","micro-ondes","ready meal","meal prep","salade composée","sandwich","quiche","tarte salée","pizza","lasagne","hachis","gratin","taboulé","couscous"),
        listOf(
            ProductSubcategory("sandwichs","Sandwichs",listOf("sandwich","wrap","panini","burger")),
            ProductSubcategory("pizzas","Pizzas",listOf("pizza")),
            ProductSubcategory("salades","Salades",listOf("salade composée","taboulé"))
        )
    ),
    ProductCategory(
        "conserves","Conserves","Archive",
        listOf("conserve","boîte","bocal","en conserve","canned","légumes en conserve","compote","confiture","cornichon","olive","tomate pelée","concentré","cassoulet"),
        listOf(
            ProductSubcategory("confitures","Confitures",listOf("confiture","jam","marmelade","compote")),
            ProductSubcategory("legumes","Légumes",listOf("légumes en conserve","cornichon","olive","tomate pelée","concentré","maïs")),
            ProductSubcategory("plats","Plats",listOf("cassoulet","ravioli en conserve","plat en conserve"))
        )
    ),
    ProductCategory(
        "epicerie","Épicerie","Wheat",
        listOf("pâtes","pâte","riz","céréales","farine","sucre","sel","biscuit","gâteau","chocolat","miel","nouilles","semoule","quinoa","lentilles","pois chiches","haricots secs","cookie","crackers","chips","snack","bonbon","céréale","pasta","rice","cereal","granola","muesli"),
        listOf(
            ProductSubcategory("pates","Pâtes",listOf("pâtes","pasta","nouilles","spaghetti","penne","tagliatelle","ravioli")),
            ProductSubcategory("riz","Riz",listOf("riz","rice","basmati","risotto")),
            ProductSubcategory("cereales","Céréales",listOf("céréales","céréale","cereal","granola","muesli","cornflakes","flocons d'avoine")),
            ProductSubcategory("biscuits","Biscuits",listOf("biscuit","cookie","crackers","gâteau","bonbon","chocolat","snack","chips")),
            ProductSubcategory("legumineuses","Légumineuses",listOf("lentilles","pois chiches","haricots secs","quinoa","semoule"))
        )
    ),
    ProductCategory("bebe","Bébé","Baby",listOf("bébé","baby","infantile","petit pot","lait infantile","compote bébé","céréales bébé","purée bébé"),emptyList()),
    ProductCategory(
        "hygiene","Hygiène & Beauté","Sparkles",
        listOf("shampooing","gel douche","savon","dentifrice","déodorant","crème solaire","lotion","maquillage","hygiène","beauté","cosmétique","soin","rasoir","coton","lingette"),
        listOf(
            ProductSubcategory("bouche","Bouche",listOf("dentifrice","bain de bouche","brosse à dents")),
            ProductSubcategory("visage","Visage",listOf("crème visage","maquillage","démaquillant","lotion visage","masque visage")),
            ProductSubcategory("corps","Corps",listOf("shampooing","gel douche","savon","déodorant","crème solaire","lotion","rasoir","coton","lingette"))
        )
    ),
    ProductCategory("autre","Autre","Package",emptyList(),emptyList())
)

val RECOMMENDED_DAYS_AFTER_OPENING: Map<String, Pair<Int, String>> = mapOf(
    "fruits_legumes" to (3 to "Fruits & Légumes : 2-4 jours"),
    "laitiers" to (3 to "Laitiers : 2-4 jours"),
    "viandes" to (2 to "Viandes : 1-2 jours"),
    "poissons" to (1 to "Poissons : 1 jour"),
    "boulangerie" to (3 to "Boulangerie : 2-4 jours"),
    "oeufs" to (2 to "Œufs : 1-2 jours (si cuits)"),
    "condiments" to (30 to "Condiments : 1-3 mois"),
    "boissons" to (5 to "Boissons : 3-7 jours"),
    "surgeles" to (2 to "Surgelés (décongelés) : 1-2 jours"),
    "plats_prepares" to (2 to "Plats préparés : 1-2 jours"),
    "conserves" to (5 to "Conserves : 3-5 jours"),
    "epicerie" to (14 to "Épicerie : 7-30 jours"),
    "bebe" to (1 to "Bébé : 24 heures"),
    "hygiene" to (180 to "Hygiène : 6-12 mois"),
    "autre" to (3 to "Général : 3 jours")
)

val FREEZE_MONTHS_BY_CATEGORY: Map<String, Int> = mapOf(
    "viandes" to 6,
    "poissons" to 6,
    "laitiers" to 3,
    "boulangerie" to 3,
    "fruits_legumes" to 12,
    "oeufs" to 3,
    "plats_prepares" to 3,
    "surgeles" to 12,
    "epicerie" to 6
)

fun getFreezeDuration(categoryKey: String?): Int = FREEZE_MONTHS_BY_CATEGORY[categoryKey.orEmpty()] ?: 3

val SUBCATEGORY_DAYS_AFTER_OPENING: Map<String, Pair<Int, String>> = mapOf(
    "fruits_legumes:Fruits" to (4 to "Fruits : 3-5 jours"),
    "fruits_legumes:Légumes" to (4 to "Légumes : 3-5 jours"),
    "fruits_legumes:Agrumes" to (7 to "Agrumes : 1 semaine"),
    "fruits_legumes:Herbes" to (2 to "Herbes : 1-3 jours"),
    "laitiers:Fromages" to (7 to "Fromages : 5-10 jours"),
    "laitiers:Yaourts" to (2 to "Yaourts : 1-2 jours"),
    "laitiers:Beurres" to (21 to "Beurres : 2-3 semaines"),
    "laitiers:Crèmes" to (3 to "Crèmes : 2-4 jours"),
    "laitiers:Laits" to (4 to "Laits : 3-5 jours"),
    "viandes:Volaille" to (2 to "Volaille : 1-2 jours"),
    "viandes:Bœuf" to (3 to "Bœuf : 2-3 jours"),
    "viandes:Porc" to (2 to "Porc : 1-2 jours"),
    "viandes:Charcuterie" to (5 to "Charcuterie : 3-5 jours"),
    "poissons:Poissons" to (1 to "Poissons : 1 jour"),
    "poissons:Crustacés" to (1 to "Crustacés : 1 jour"),
    "poissons:Fumés" to (3 to "Fumés : 2-3 jours"),
    "boulangerie:Pains" to (3 to "Pains : 2-3 jours"),
    "boulangerie:Viennoiseries" to (2 to "Viennoiseries : 1-2 jours"),
    "boissons:Jus" to (4 to "Jus : 3-5 jours"),
    "boissons:Sodas" to (3 to "Sodas : 2-3 jours"),
    "boissons:Cafés" to (2 to "Cafés/Thés : 1-2 jours"),
    "boissons:Alcools" to (30 to "Alcools : plusieurs semaines"),
    "epicerie:Pâtes" to (5 to "Pâtes (cuites) : 3-5 jours"),
    "epicerie:Riz" to (4 to "Riz (cuit) : 3-4 jours"),
    "epicerie:Biscuits" to (14 to "Biscuits : 1-2 semaines"),
    "epicerie:Céréales" to (30 to "Céréales : 1 mois"),
    "epicerie:Légumineuses" to (4 to "Légumineuses (cuites) : 3-5 jours"),
    "condiments:Sauces" to (30 to "Sauces : 1 mois"),
    "condiments:Huiles" to (180 to "Huiles : 6 mois"),
    "condiments:Vinaigres" to (365 to "Vinaigres : 1 an"),
    "condiments:Épices" to (365 to "Épices : 1 an"),
    "surgeles:Glaces" to (30 to "Glaces : 1 mois (au congélateur)"),
    "surgeles:Légumes" to (2 to "Légumes (décongelés) : 1-2 jours"),
    "surgeles:Plats" to (2 to "Plats (décongelés) : 1-2 jours"),
    "plats_prepares:Sandwichs" to (1 to "Sandwichs : 24h"),
    "plats_prepares:Pizzas" to (2 to "Pizzas : 1-2 jours"),
    "plats_prepares:Salades" to (1 to "Salades : 24h"),
    "conserves:Légumes" to (4 to "Légumes : 3-5 jours"),
    "conserves:Confitures" to (30 to "Confitures : 1 mois"),
    "conserves:Plats" to (3 to "Plats : 2-3 jours"),
    "hygiene:Corps" to (180 to "Corps : 6-12 mois"),
    "hygiene:Visage" to (180 to "Visage : 6 mois"),
    "hygiene:Bouche" to (90 to "Bouche : 3 mois")
)

fun getRecommendedDaysAfterOpening(categoryKey: String?, subcategoryLabel: String?): Pair<Int, String> {
    if (categoryKey != null && subcategoryLabel != null) {
        val key = "$categoryKey:$subcategoryLabel"
        SUBCATEGORY_DAYS_AFTER_OPENING[key]?.let { return it }
    }
    return RECOMMENDED_DAYS_AFTER_OPENING[categoryKey] ?: (3 to "Général : 3 jours")
}

val CATEGORY_POST_EXPIRY_NOTES: Map<String, String?> = mapOf(
    "fruits_legumes" to "quelques jours",
    "laitiers" to "1 semaine",
    "viandes" to null,
    "poissons" to null,
    "boulangerie" to "1 à 2 jours",
    "oeufs" to "4 semaines",
    "condiments" to "2 à 6 mois",
    "boissons" to "2 à 3 mois",
    "surgeles" to "2 à 3 mois",
    "plats_prepares" to null,
    "conserves" to "1 à 2 ans",
    "epicerie" to "1 à 3 mois",
    "bebe" to null,
    "hygiene" to "6 à 12 mois",
    "autre" to null
)

val SUBCATEGORY_POST_EXPIRY_NOTES: Map<String, String?> = mapOf(
    "fruits_legumes:Fruits" to "3 à 5 jours",
    "fruits_legumes:Légumes" to "3 à 5 jours",
    "fruits_legumes:Agrumes" to "1 à 2 semaines",
    "fruits_legumes:Herbes" to "2 à 3 jours",
    "laitiers:Fromages" to "1 à 2 semaines",
    "laitiers:Yaourts" to "2 à 3 semaines",
    "laitiers:Beurres" to "1 mois",
    "laitiers:Crèmes" to "3 à 5 jours",
    "laitiers:Laits" to "4 à 7 jours",
    "viandes:Volaille" to null,
    "viandes:Bœuf" to "1 à 2 jours",
    "viandes:Porc" to null,
    "viandes:Charcuterie" to "3 à 5 jours",
    "poissons:Poissons" to null,
    "poissons:Crustacés" to null,
    "poissons:Fumés" to "2 à 3 jours",
    "boulangerie:Pains" to "2 à 3 jours",
    "boulangerie:Viennoiseries" to "1 jour",
    "boissons:Jus" to "1 à 2 semaines",
    "boissons:Sodas" to "3 à 6 mois",
    "boissons:Cafés" to "6 à 12 mois",
    "boissons:Alcools" to "plusieurs années",
    "epicerie:Pâtes" to "1 à 2 ans",
    "epicerie:Riz" to "1 à 2 ans",
    "epicerie:Biscuits" to "1 à 3 mois",
    "epicerie:Céréales" to "3 à 6 mois",
    "epicerie:Légumineuses" to "1 à 2 ans",
    "condiments:Sauces" to "1 à 3 mois",
    "condiments:Huiles" to "6 à 12 mois",
    "condiments:Vinaigres" to "plusieurs années",
    "condiments:Épices" to "1 à 2 ans",
    "surgeles:Glaces" to "2 à 3 mois",
    "surgeles:Légumes" to "6 à 12 mois",
    "surgeles:Plats" to "2 à 3 mois",
    "plats_prepares:Sandwichs" to null,
    "plats_prepares:Pizzas" to "1 à 2 jours",
    "plats_prepares:Salades" to null,
    "conserves:Légumes" to "1 à 2 ans",
    "conserves:Confitures" to "1 à 2 ans",
    "conserves:Plats" to "1 an",
    "hygiene:Corps" to "6 à 12 mois",
    "hygiene:Visage" to "3 à 6 mois",
    "hygiene:Bouche" to "3 à 6 mois"
)

fun getPostExpiryNote(categoryKey: String?, subcategoryLabel: String?): String? {
    if (categoryKey != null && subcategoryLabel != null) {
        val key = "$categoryKey:$subcategoryLabel"
        if (key in SUBCATEGORY_POST_EXPIRY_NOTES) return SUBCATEGORY_POST_EXPIRY_NOTES[key]
    }
    return CATEGORY_POST_EXPIRY_NOTES[categoryKey]
}
