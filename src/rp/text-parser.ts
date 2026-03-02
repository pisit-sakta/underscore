/**
 * RP text parser.
 *
 * Cleans and structures roleplay text for scene classification.
 * Handles common RP conventions: *action text*, "dialogue",
 * Character: prefixes, (( OOC markers )).
 */
import type { TextContext } from "../core/types.js";

/** Known franchise keywords for quick detection */
const FRANCHISE_KEYWORDS: Record<string, string[]> = {
  "Genshin Impact": [
    "liyue", "mondstadt", "inazuma", "sumeru", "fontaine", "natlan",
    "vision", "archon", "elemental", "traveler", "abyss order",
    "zhongli", "raiden", "venti", "nahida", "furina",
  ],
  "Metal Gear Solid": [
    "mother base", "big boss", "snake", "phantom pain", "diamond dogs",
    "outer heaven", "foxhound", "metal gear", "ocelot", "quiet",
  ],
  "Re:Zero": [
    "subaru", "emilia", "rem", "ram", "roswaal", "witch",
    "return by death", "lugunica", "reinhard",
  ],
  "Ace Attorney": [
    "objection", "hold it", "take that", "wright", "edgeworth",
    "prosecutor", "courtroom", "evidence", "testimony", "pursuit",
  ],
  "Persona": [
    "persona", "shadow", "palace", "metaverse", "velvet room",
    "social link", "confidant", "joker", "phantom thieves",
  ],
  "Yakuza": [
    "kiryu", "kamurocho", "majima", "tojo clan", "omi alliance",
    "yakuza", "dragon of dojima", "dame da ne",
  ],
  "Overlord": [
    "ainz", "nazarick", "great tomb", "floor guardian",
    "supreme being", "albedo", "demiurge", "shalltear",
  ],
};

/** Combat-related keywords */
const COMBAT_KEYWORDS = [
  "attack", "fight", "battle", "sword", "strike", "dodge",
  "spell", "magic", "shoot", "fire", "explosion", "weapon",
  "defend", "shield", "parry", "slash", "stab", "punch", "kick",
  "combat", "clash", "duel", "charge", "arrow", "blast",
  "wound", "bleed", "damage", "hp", "health",
];

/**
 * Parse raw RP text into structured context for the scene classifier.
 */
export function parseText(
  rawText: string,
  recentMessages: string[] = []
): TextContext {
  // Strip OOC markers
  const cleaned = rawText.replace(/\(\(.*?\)\)/gs, "").trim();

  // Detect franchise
  const detectedFranchise = detectFranchise(cleaned);

  // Extract character names (look for Name: patterns and *Name action* patterns)
  const charactersMentioned = extractCharacters(cleaned);

  // Detect combat
  const lowerText = cleaned.toLowerCase();
  const hasCombat = COMBAT_KEYWORDS.some((kw) => lowerText.includes(kw));

  return {
    rawText: cleaned,
    detectedFranchise,
    charactersMentioned,
    hasCombat,
    recentMessages,
  };
}

/** Detect franchise from text keywords */
function detectFranchise(text: string): string | null {
  const lower = text.toLowerCase();
  let bestMatch: string | null = null;
  let bestScore = 0;

  for (const [franchise, keywords] of Object.entries(FRANCHISE_KEYWORDS)) {
    const score = keywords.filter((kw) => lower.includes(kw)).length;
    if (score > bestScore) {
      bestScore = score;
      bestMatch = franchise;
    }
  }

  return bestScore >= 1 ? bestMatch : null;
}

/** Extract character names from common RP formatting patterns */
function extractCharacters(text: string): string[] {
  const names = new Set<string>();

  // Pattern: "CharacterName:" at the start of a line
  const colonPattern = /^([A-Z][a-zA-Z\s]{1,30}):/gm;
  for (const match of text.matchAll(colonPattern)) {
    names.add(match[1].trim());
  }

  // Pattern: *CharacterName verbs...* (action text)
  const actionPattern = /\*([A-Z][a-zA-Z]+)\s/g;
  for (const match of text.matchAll(actionPattern)) {
    names.add(match[1]);
  }

  return [...names];
}
