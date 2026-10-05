export const PERSONALITY_CONTENT_VERSION = '1.0'

export type PersonalityLetter =
  | 'E'
  | 'I'
  | 'S'
  | 'N'
  | 'T'
  | 'F'
  | 'J'
  | 'P'

export type PersonalityTypeCode =
  | 'ISTJ'
  | 'ISFJ'
  | 'INFJ'
  | 'INTJ'
  | 'ISTP'
  | 'ISFP'
  | 'INFP'
  | 'INTP'
  | 'ESTP'
  | 'ESFP'
  | 'ENFP'
  | 'ENTP'
  | 'ESTJ'
  | 'ESFJ'
  | 'ENFJ'
  | 'ENTJ'

export interface PersonalityLetterDescription {
  letter: PersonalityLetter
  name: string
  coreMeaning: string
  tendencies: string[]
  doesNotMean: string[]
}

export interface PersonalityTypeDescription {
  typeCode: PersonalityTypeCode
  coreTendency: string
  strengths: string[]
  blindSpots: string[]
}

export const personalityLetterDescriptions: Record<
  PersonalityLetter,
  PersonalityLetterDescription
> = {
  E: {
    letter: 'E',
    name: 'Extraversion',
    coreMeaning:
      'A preference for engaging with the external world to gain stimulation, organize thoughts, and become involved in the environment.',
    tendencies: [
      'develop ideas by discussing them with other people',
      'become involved in group interaction relatively quickly',
      'gain energy from communication, activities, or the external environment',
      'move a problem forward through interaction rather than extended private reflection',
    ],
    doesNotMean: [
      'always being talkative',
      'always enjoying large social gatherings',
      'having stronger leadership ability than I',
      'never needing time alone',
    ],
  },
  I: {
    letter: 'I',
    name: 'Introversion',
    coreMeaning:
      'A preference for organizing thoughts internally and having more private space to recover attention and energy.',
    tendencies: [
      'form ideas internally before expressing them',
      'observe a new group before becoming actively involved',
      'prefer fewer but deeper interactions',
      'need time alone after extended social activity',
      'work or think independently for long periods',
    ],
    doesNotMean: [
      'being shy',
      'having poor social skills',
      'disliking people',
      'being unsuited for leadership or teamwork',
    ],
  },
  S: {
    letter: 'S',
    name: 'Sensing',
    coreMeaning:
      'A preference for directly observable information, concrete facts, and practical feasibility.',
    tendencies: [
      'understand new concepts by starting with concrete examples',
      'focus on facts and information that are already available',
      'notice practical details',
      'prefer methods that have already been shown to work',
      'want to understand how an idea operates in practice',
    ],
    doesNotMean: [
      'lacking imagination',
      'being poor at innovation',
      'being able to focus only on details',
      'being unable to understand abstract concepts',
    ],
  },
  N: {
    letter: 'N',
    name: 'Intuition',
    coreMeaning:
      'A preference for patterns, possibilities, abstract concepts, and future development.',
    tendencies: [
      'look for hidden connections between things',
      'think about what may happen in the future',
      'be interested in abstract concepts for their own sake',
      'connect ideas from different fields',
      'explore new approaches that have not yet been validated',
    ],
    doesNotMean: [
      'being unrealistic',
      'ignoring facts',
      'automatically being more creative',
      'being more intelligent than S',
    ],
  },
  T: {
    letter: 'T',
    name: 'Thinking',
    coreMeaning:
      'When making judgments, a preference for starting with logical consistency, causal relationships, and problem-solving considerations.',
    tendencies: [
      'examine whether an argument is logically consistent',
      'compare the advantages and disadvantages of alternatives',
      'first look for what can be fixed in a problem',
      'temporarily separate personal feelings from analytical evaluation',
      'accept direct and explicit feedback',
    ],
    doesNotMean: [
      'having no empathy',
      'not caring about other people',
      'having no emotions',
      'always being more rational or correct',
    ],
  },
  F: {
    letter: 'F',
    name: 'Feeling',
    coreMeaning:
      'When making judgments, a preference for considering people\'s experiences, values, and relationship impact alongside other factors.',
    tendencies: [
      'consider how a decision will affect the people involved',
      'adjust communication to account for other people\'s feelings',
      'include trust, relationships, and fairness in decision making',
      'consider both parties\' experiences when resolving conflict',
      'first try to understand how someone feels and sees the situation when hearing about a problem',
    ],
    doesNotMean: [
      'ignoring logic',
      'being overly emotional',
      'being unable to make difficult decisions',
      'being kinder than T',
    ],
  },
  J: {
    letter: 'J',
    name: 'Judging',
    coreMeaning:
      'A preference for establishing structure, plans, and certainty relatively early.',
    tendencies: [
      'prefer settling important plans in advance',
      'want a clear next step',
      'prefer completing important tasks first',
      'feel uncomfortable when important decisions remain unresolved for a long time',
      'move work forward more comfortably when the structure and endpoint are clear',
    ],
    doesNotMean: [
      'judging or criticizing other people',
      'lacking flexibility',
      'always being highly organized',
      'being unable to accept change',
    ],
  },
  P: {
    letter: 'P',
    name: 'Perceiving',
    coreMeaning:
      'A preference for keeping options open and adjusting direction as new information appears.',
    tendencies: [
      'keep several possible options available',
      'adapt relatively quickly when plans change',
      'discover the best direction gradually while acting',
      'wait for more information rather than deciding too early',
      'prefer plans that leave some room for spontaneity',
    ],
    doesNotMean: [
      'being irresponsible',
      'being unable to make plans',
      'procrastinating',
      'lacking goals',
    ],
  },
}

export const personalityTypeDescriptions: Record<
  PersonalityTypeCode,
  PersonalityTypeDescription
> = {
  ISTJ: {
    typeCode: 'ISTJ',
    coreTendency:
      'ISTJs may prefer to handle practical problems in quiet, clear, and structured environments. They tend to first understand the facts and concrete requirements, then use consistent logic and planning to complete work reliably.',
    strengths: [
      'strong sense of responsibility and commitment',
      'good at maintaining stable processes',
      'attentive to practical details',
      'comfortable breaking complex tasks into clear steps',
      'often consistent in long-term execution and follow-through',
    ],
    blindSpots: [
      'may need more time to adapt to highly ambiguous or rapidly changing environments',
      'may rely too heavily on methods that have already been proven',
      'may underestimate new possibilities that are not yet validated but still worth exploring',
    ],
  },
  ISFJ: {
    typeCode: 'ISFJ',
    coreTendency:
      'ISFJs may be quiet and detail-oriented while paying close attention to practical responsibilities and how those responsibilities affect other people. They often prefer supporting others through reliable, concrete action.',
    strengths: [
      'attentive to details',
      'sensitive to other people\'s needs',
      'values commitment and stability',
      'good at providing consistent support within an existing structure',
      'often remembers important information related to people and practical tasks',
    ],
    blindSpots: [
      'may take on too much responsibility and find it difficult to say no',
      'may be cautious when established methods need to be changed quickly',
      'may sometimes prioritize harmony over expressing personal needs',
    ],
  },
  INFJ: {
    typeCode: 'INFJ',
    coreTendency:
      'INFJs may prefer using quiet reflection to look for meaning across people, events, and long-term developments. They often want their actions to align with internal values while also moving toward a clear direction.',
    strengths: [
      'good at seeing patterns and long-term implications',
      'able to connect abstract ideas with human needs',
      'sensitive to values, motivation, and relationships',
      'often enjoys long-term planning around meaningful goals',
      'tends to value depth over surface-level interaction',
    ],
    blindSpots: [
      'may set very high standards for themselves or ideal goals',
      'may sometimes overinterpret hidden meanings',
      'may experience strong frustration when reality differs significantly from an ideal',
    ],
  },
  INTJ: {
    typeCode: 'INTJ',
    coreTendency:
      'INTJs may prefer independently analyzing complex problems, identifying the underlying structure and long-term direction, and then forming a clear strategy. They often care more about whether a system makes sense than whether it follows tradition.',
    strengths: [
      'strong at long-term planning',
      'good at identifying structures and weaknesses in systems',
      'comfortable handling complex problems independently',
      'often interested in improving inefficient processes',
      'tends to value logical consistency when making decisions',
    ],
    blindSpots: [
      'may dismiss immature ideas too quickly as inefficient',
      'may underestimate the importance of emotion, relationships, or organizational culture',
      'may become impatient when execution quality does not meet expectations',
    ],
  },
  ISTP: {
    typeCode: 'ISTP',
    coreTendency:
      'ISTPs may prefer independently observing practical problems, analyzing them quickly when necessary, and finding workable solutions. They often prefer understanding directly how a system works rather than following too many predefined steps.',
    strengths: [
      'calm when dealing with practical problems',
      'good at troubleshooting',
      'sensitive to tools, systems, and cause-and-effect relationships',
      'able to adjust flexibly to conditions on the ground',
      'under pressure, often focuses first on the immediate problem that can be solved',
    ],
    blindSpots: [
      'may dislike extensive long-term planning',
      'may sometimes neglect communication and relationship maintenance',
      'may be interested in theoretical discussion only after seeing a practical use for it',
    ],
  },
  ISFP: {
    typeCode: 'ISFP',
    coreTendency:
      'ISFPs may be quiet, value personal authenticity, and pay close attention to immediate real-world experience. They often prefer maintaining freedom and doing meaningful things in their own way without excessive external control.',
    strengths: [
      'sensitive to the concrete environment and other people\'s feelings',
      'often adaptable',
      'values authenticity and personal values',
      'expresses care through concrete action',
      'may be especially aware of changes in experience, aesthetics, or detail',
    ],
    blindSpots: [
      'may dislike making long-term commitments too early',
      'may feel constrained in highly structured environments',
      'may sometimes avoid direct conflict, leaving personal needs insufficiently expressed',
    ],
  },
  INFP: {
    typeCode: 'INFP',
    coreTendency:
      'INFPs may place strong value on personal meaning, values, and possibilities. They often reflect internally on what matters and explore whether reality could become more aligned with those values.',
    strengths: [
      'sensitive to meaning and values',
      'imaginative',
      'often able to understand different people\'s inner perspectives',
      'deeply invested in issues they personally believe in',
      'able to see possibilities beyond the current situation',
    ],
    blindSpots: [
      'may delay decisions when too many options remain open',
      'may spend significant energy when real-world demands conflict with personal values',
      'may invest heavily in an ideal state while underestimating execution details',
    ],
  },
  INTP: {
    typeCode: 'INTP',
    coreTendency:
      'INTPs may prefer independently studying concepts, systems, and logical models. They are often more interested in "why does this work this way?" than "how has this always been done?" and may hold back a conclusion until they believe the evidence is sufficient.',
    strengths: [
      'strong at abstract analysis',
      'enjoys identifying the principles behind systems',
      'sensitive to contradictions and logical gaps',
      'able to redefine a problem from multiple perspectives',
      'often highly curious about new theories and complex concepts',
    ],
    blindSpots: [
      'may remain in the analysis phase for too long',
      'may lose interest in repetitive execution and final detail work',
      'may underestimate the role of emotion, organizational habits, or interpersonal factors in real-world decisions',
    ],
  },
  ESTP: {
    typeCode: 'ESTP',
    coreTendency:
      'ESTPs may prefer engaging directly with the real environment and solving problems through action, observation, and immediate feedback. They often adapt quickly to change and are willing to try a workable option before every piece of information is available.',
    strengths: [
      'action-oriented',
      'reacts quickly to changes in the environment',
      'good at identifying practical problems rapidly',
      'flexible when improvisation is required',
      'often engages quickly with both people and surroundings',
    ],
    blindSpots: [
      'may underestimate long-term consequences',
      'may feel constrained by situations requiring extensive advance planning',
      'may act too quickly before fully considering the feelings of everyone involved',
    ],
  },
  ESFP: {
    typeCode: 'ESFP',
    coreTendency:
      'ESFPs may gain energy from active engagement with the real environment and pay close attention to current experiences and the reactions of people around them. They often enjoy making activities lively, human, and participatory.',
    strengths: [
      'sensitive to immediate reactions from both people and the environment',
      'good at creating a relaxed interaction atmosphere',
      'adapts quickly to change',
      'often enjoys helping people through concrete action',
      'usually values direct, real experiences',
    ],
    blindSpots: [
      'may have less patience for long-term abstract planning',
      'may focus on current needs while underestimating future constraints',
      'excessive structure and rules may significantly reduce engagement',
    ],
  },
  ENFP: {
    typeCode: 'ENFP',
    coreTendency:
      'ENFPs may gain energy from people and new possibilities and enjoy exploring connections between different ideas. They often want their actions to feel meaningful while preserving the freedom to change direction.',
    strengths: [
      'generates new ideas easily',
      'sensitive to both people and possibilities',
      'good at stimulating discussion and creativity',
      'quickly sees connections between different fields',
      'usually willing to try new directions',
    ],
    blindSpots: [
      'too many new ideas may fragment attention',
      'may lose interest in repetitive execution and long-term detail management',
      'may be attracted to the next possibility before fully completing the current project',
    ],
  },
  ENTP: {
    typeCode: 'ENTP',
    coreTendency:
      'ENTPs may enjoy understanding problems through discussion, experimentation, and challenging existing assumptions. They are often highly interested in new models, possibilities, and more effective ways of solving problems.',
    strengths: [
      'generates alternatives quickly',
      'good at identifying assumptions and logical gaps',
      'enjoys complex conceptual problem solving',
      'can adjust viewpoints rapidly during discussion',
      'usually not constrained by "this is how we have always done it."',
    ],
    blindSpots: [
      'may keep changing direction in the name of exploration',
      'may sometimes treat intellectual debate as if it were the problem-solving outcome itself',
      'may lose patience with stable process maintenance and repetitive execution',
    ],
  },
  ESTJ: {
    typeCode: 'ESTJ',
    coreTendency:
      'ESTJs may prefer organizing practical work into clear responsibilities, processes, and goals. They are often willing to move things forward directly and place high value on results, efficiency, and predictability.',
    strengths: [
      'good at organizing resources and tasks',
      'often makes decisions quickly',
      'values clear responsibility',
      'able to move teams from discussion into execution',
      'sensitive to practical constraints and operational details',
    ],
    blindSpots: [
      'may turn ambiguous problems into fixed processes too quickly',
      'may underestimate individual differences or emotional factors',
      'may become too eager to close decisions in situations that require extended open exploration',
    ],
  },
  ESFJ: {
    typeCode: 'ESFJ',
    coreTendency:
      'ESFJs may prefer creating stable environments through cooperation, relationship maintenance, and reliable execution of practical responsibilities. They often pay close attention to the needs, expectations, and shared norms of a group.',
    strengths: [
      'good at coordinating people and practical tasks',
      'sensitive to other people\'s immediate needs',
      'values reliability and responsibility',
      'often takes initiative in maintaining a positive group atmosphere',
      'can usually move work forward consistently when structure is clear',
    ],
    blindSpots: [
      'may depend too much on external feedback',
      'may experience stress when required to make unpopular decisions',
      'may sometimes delay necessary conflict in order to preserve harmony',
    ],
  },
  ENFJ: {
    typeCode: 'ENFJ',
    coreTendency:
      'ENFJs may focus on people and future possibilities and may actively organize others toward a meaningful direction. They often want the goal itself to matter while also wanting team members to participate and grow.',
    strengths: [
      'good at understanding and coordinating different people\'s needs',
      'able to turn an abstract vision into a shared direction',
      'often strong at communication and mobilizing others',
      'values long-term relationships and team development',
      'enjoys establishing clear goals for a group',
    ],
    blindSpots: [
      'may take on too many of other people\'s problems',
      'may suppress personal needs to preserve group alignment',
      'may become disappointed when expectations for other people\'s potential are too high',
    ],
  },
  ENTJ: {
    typeCode: 'ENTJ',
    coreTendency:
      'ENTJs may prefer rapidly understanding complex systems, defining long-term goals, and organizing resources to drive execution. They often place significant value on efficiency, strategy, and results.',
    strengths: [
      'strong at strategic planning',
      'able to organize complex work quickly',
      'sensitive to systemic inefficiency',
      'willing to take responsibility for decisions',
      'able to turn abstract goals into action structures',
    ],
    blindSpots: [
      'may underestimate other people\'s acceptance process when prioritizing efficiency',
      'may become impatient in slow-moving environments or situations without clear direction',
      'may need to deliberately make more room for relationships, emotions, and non-quantifiable factors',
    ],
  },
}

export function isPersonalityTypeCode(
  value: string,
): value is PersonalityTypeCode {
  return value in personalityTypeDescriptions
}

export function getPersonalityTypeDescription(
  typeCode: string,
): PersonalityTypeDescription | null {
  return isPersonalityTypeCode(typeCode)
    ? personalityTypeDescriptions[typeCode]
    : null
}

export function getPersonalityLetterDescriptions(
  typeCode: PersonalityTypeCode,
): PersonalityLetterDescription[] {
  return typeCode
    .split('')
    .map(
      (letter) =>
        personalityLetterDescriptions[
          letter as PersonalityLetter
        ],
    )
}
