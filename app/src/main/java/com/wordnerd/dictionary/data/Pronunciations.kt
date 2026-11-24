package com.wordnerd.dictionary.data

/**
 * How the speaker button should say a word.
 *
 * The phone's text-to-speech voice guesses pronunciation from spelling, which goes wrong
 * for many words here. For those, [english] is a respelling an English voice can't
 * misread (lowercase sound-chunks, e.g. "vittles"). Loanwords that English keeps close to
 * the original also get a [locale] and optional [native] text (in the native script where
 * needed), read by that language's voice when the phone has it; otherwise [english] is used.
 *
 * Words not listed are read from their normal spelling.
 */
data class Spoken(
    val english: String? = null,
    val locale: String? = null,
    val native: String? = null,
)

private fun en(respelling: String) = Spoken(english = respelling)
private fun de(en: String, text: String? = null) = Spoken(en, "de-DE", text)
private fun fr(en: String, text: String? = null) = Spoken(en, "fr-FR", text)
private fun it(en: String, text: String? = null) = Spoken(en, "it-IT", text)
private fun es(en: String, text: String? = null) = Spoken(en, "es-ES", text)
private fun ja(en: String, text: String) = Spoken(en, "ja-JP", text)

fun spokenFor(word: Word): Spoken = SPOKEN[word.id] ?: Spoken()

/** Keyed by [Word.id] (the lowercase headword). */
internal val SPOKEN: Map<String, Spoken> = mapOf(
    // ── Read by a native voice when available ────────────────────────────
    // German
    "schadenfreude" to de("shah den froy duh"),
    "weltschmerz" to de("velt shmerts"),
    "sehnsucht" to de("zane zookht"),
    "gemütlichkeit" to de("geh mute lish kite"),
    "torschlusspanik" to de("tor shloos pah nik"),
    "zeitgeist" to de("tsite guyst"),
    "weltanschauung" to de("velt ahn show ung"),
    "gestalt" to de("guh shtalt"),
    "fernweh" to de("fairn vay"),
    "bildungsroman" to de("bill doongs roh mahn"),
    // French
    "ennui" to fr("on wee"),
    "joie de vivre" to fr("zhwah duh veev ruh"),
    "frisson" to fr("free sohn"),
    "sangfroid" to fr("sahn fwah"),
    "élan" to fr("ay lahn"),
    "bon vivant" to fr("bon vee vahn"),
    "enfant terrible" to fr("on fon teh ree bluh"),
    "éminence grise" to fr("ay mee nahnss greez"),
    "flâneur" to fr("flah nur"),
    "dérive" to fr("day reev"),
    "mise en place" to fr("meez on plahss"),
    "mise en abyme" to fr("meez on ah beem"),
    "amuse-bouche" to fr("ah mooz boosh"),
    "digestif" to fr("dee zhes teef"),
    "terroir" to fr("tair wahr"),
    "contretemps" to fr("kon truh tahn"),
    "esprit de l'escalier" to fr("es pree duh less kal yay"),
    "jamais vu" to fr("zha may voo"),
    "folie à deux" to fr("foh lee ah duh"),
    "roman à clef" to fr("roh mahn ah klay"),
    "sillage" to fr("see yahzh"),
    // Italian
    "fata morgana" to it("fah tuh mor gah nuh"),
    "dolce far niente" to it("dohl chay far nee en tay"),
    "sotto voce" to it("sot oh voh chay"),
    "sprezzatura" to it("spret suh too ruh"),
    // Spanish
    "querencia" to es("keh ren see ah"),
    "duende" to es("doo en day"),
    "mañana" to es("mahn yah nuh"),
    "sobremesa" to es("soh breh meh sah"),
    // Others
    "saudade" to Spoken("sow dah djee", "pt-BR"),
    "hygge" to Spoken("hoo guh", "da-DK"),
    "lagom" to Spoken("lah gom", "sv-SE"),
    "sisu" to Spoken("see soo", "fi-FI"),
    "toska" to Spoken("toss kuh", "ru-RU", "тоска"),
    "meraki" to Spoken("meh rah kee", "el-GR", "μεράκι"),
    // Japanese (native script, since a Japanese voice can't read romaji reliably)
    "komorebi" to ja("koh moh reh bee", "木漏れ日"),
    "tsundoku" to ja("tsoon doh koo", "積ん読"),
    "wabi-sabi" to ja("wah bee sah bee", "侘び寂び"),
    "mono no aware" to ja("moh noh noh ah wah reh", "もののあわれ"),
    "ikigai" to ja("ee kee guy", "生きがい"),
    "omakase" to ja("oh mah kah say", "おまかせ"),

    // ── English respellings ──────────────────────────────────────────────
    // Silent letters and surprising spellings
    "victuals" to en("vittles"),
    "deosil" to en("jeshul"),
    "apothegm" to en("appa them"),
    "sobriquet" to en("soh brih kay"),
    "sennight" to en("sen nite"),
    "betimes" to en("be times"),
    "yclept" to en("ih klept"),
    "wiseacre" to en("wise acre"),
    "gewgaw" to en("gyoo gaw"),
    "petard" to en("pih tard"),
    "gourmand" to en("goor mahnd"),
    "dishabille" to en("dis uh beel"),
    "insouciance" to en("in soo see ince"),
    "raconteur" to en("rack on tur"),
    "aperitif" to en("uh pair uh teef"),
    "doppelgänger" to en("doppel ganger"),
    "smörgåsbord" to en("smor gus bord"),
    "nom de plume" to en("nom duh ploom"),
    "anomie" to en("anna mee"),
    "antipodes" to en("an tip uh deez"),
    "litotes" to en("lye tuh teez"),
    "cacoethes" to en("cack oh ee theez"),
    "abecedarian" to en("A B C dairian"),
    // Greek and Latin spellings an English voice tends to misread
    "aestival" to en("estival"),
    "aestivate" to en("estivate"),
    "ailurophile" to en("eye lure oh file"),
    "akrasia" to en("uh cray zee uh"),
    "apricity" to en("uh priss ih tee"),
    "aposiopesis" to en("ap uh sigh uh pee sis"),
    "chiasmus" to en("kye as muss"),
    "diaeresis" to en("dye air uh sis"),
    "deus ex machina" to en("day us ex mah kin uh"),
    "deipnosophist" to en("dype nossa fist"),
    "eudaimonia" to en("you dye moh nee uh"),
    "eunoia" to en("you noy uh"),
    "genius loci" to en("jeenius low sigh"),
    "gibbous" to en("gib us"),
    "hapax legomenon" to en("hap ax leh gomma non"),
    "hiemal" to en("high uh mul"),
    "hypnagogic" to en("hip nuh godge ick"),
    "in medias res" to en("in may dee ahs race"),
    "ipse dixit" to en("ip say dix it"),
    "kairos" to en("kye ross"),
    "lunule" to en("loon yool"),
    "manicule" to en("man ih kyool"),
    "meliorism" to en("meel yer ism"),
    "nival" to en("nye vul"),
    "nuchal" to en("noo kul"),
    "nudiustertian" to en("noo dee us ter shun"),
    "nychthemeron" to en("nik them er on"),
    "oenophile" to en("ee nuh file"),
    "pareidolia" to en("pair eye doh lee uh"),
    "petrichor" to en("pet rih kor"),
    "phosphene" to en("foss feen"),
    "pogonophile" to en("poh gonna file"),
    "prolix" to en("proh licks"),
    "pyrrhic" to en("peer ick"),
    "sciamachy" to en("sigh am uh kee"),
    "serotinal" to en("sih rot ih nul"),
    "sisyphean" to en("sis ih fee un"),
    "somniloquy" to en("som nil uh kwee"),
    "sophrosyne" to en("suh fross uh nee"),
    "sui generis" to en("soo ee jen er is"),
    "susurrus" to en("soo sur us"),
    "syzygy" to en("siz ih jee"),
    "tmesis" to en("tuh mee sis"),
    "ultima thule" to en("ul tih muh thoo lee"),
    "velleity" to en("veh lee ih tee"),
    "xenial" to en("zee nee ul"),
    "zeugma" to en("zoog muh"),
    "casuistry" to en("cazh oo istry"),
    "callipygian" to en("cal ih pidge ee un"),
    "brontide" to en("bron tide"),
    "caravanserai" to en("caravan suh rye"),
    "lothario" to en("loh thair ee oh"),
    "kafkaesque" to en("kahf kuh esk"),
    "mudita" to en("moo dee tuh"),
    "sastrugi" to en("sa stroo gee"),
    "graupel" to en("grow pull"),
    "imbroglio" to en("im brohl yo"),
    "cognoscenti" to en("con yuh shen tee"),
    "clinquant" to en("clink ent"),
    "hobbledehoy" to en("hobble dee hoy"),
    "snafu" to en("sna foo"),
    // Yiddish, Welsh, Yaghan and other words without a common phone voice
    "chutzpah" to en("hoots puh"),
    "kibitzer" to en("kibbitzer"),
    "nachas" to en("nah khus"),
    "schlemiel" to en("shluh meel"),
    "schlimazel" to en("shlih mah zul"),
    "shpilkes" to en("shpil kiss"),
    "tchotchke" to en("choch kuh"),
    "verklempt" to en("fer klempt"),
    "hiraeth" to en("heer eyeth"),
    "mamihlapinatapai" to en("mah mee lah pee nah tah pie"),
    // Very long words, broken up so they aren't garbled
    "floccinaucinihilipilification" to en("flock sih naw sih nye hill ih pill ih fih kay shun"),
    "honorificabilitudinitatibus" to en("on or if ih ka bil ih too din ih tah tih bus"),
    "zenzizenzizenzic" to en("zen zih zen zih zen zick"),
    "sphenopalatine ganglioneuralgia" to en("sfee noh pal uh tine gang lee oh noor al juh"),
)
