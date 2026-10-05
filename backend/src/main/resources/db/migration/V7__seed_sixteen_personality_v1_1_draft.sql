-- Expand phase for DefinitionVersion 1.1.
-- Keep 1.0 AVAILABLE during the compatibility deployment so old ECS tasks
-- cannot bind Sessions to semantics they do not understand.
-- A later activation migration will retire 1.0 and promote 1.1 only after
-- all running application tasks support both versions.

INSERT INTO assessment_definition_versions (
    id,
    definition_id,
    version_code,
    status,
    specification,
    created_at,
    published_at
)
VALUES (
           'b7a63f1e-60f4-4b5e-a0e3-1c1b1a110001',
           '6a0c8f0d-4f8a-4b8e-9d2d-7d5d0fa2b3c1',
           '1.1',
           'DRAFT',
           $spec$
               {
  "dimensions": [
    {
      "code": "EI",
           "position": 1,
           "poleA": "E",
           "poleB": "I"
    },
           {
           "code": "SN",
           "position": 2,
           "poleA": "S",
           "poleB": "N"
    },
           {
           "code": "TF",
           "position": 3,
           "poleA": "T",
           "poleB": "F"
    },
           {
           "code": "JP",
           "position": 4,
           "poleA": "J",
           "poleB": "P"
    }
  ],

           "tieBreakQuestions": [
    {
      "questionId": "TB-EI-1",
      "dimension": "EI",
      "instruction": "Both options may describe you in different situations. If you had to choose, select the one that feels more natural to you most of the time.",
      "prompt": "When you are trying to make sense of an important issue, which approach more often helps your thoughts become clear?",
      "options": [
        {
          "optionId": "TB-EI-01",
          "text": "I start discussing it with someone and often discover what I think while talking.",
          "resolvedPole": "E"
        },
        {
          "optionId": "TB-EI-02",
          "text": "I first spend some time thinking it through privately, then share my thoughts once they have taken shape.",
          "resolvedPole": "I"
        }
      ]
    },
    {
      "questionId": "TB-SN-1",
      "dimension": "SN",
      "instruction": "Both options may describe you in different situations. If you had to choose, select the one that feels more natural to you most of the time.",
      "prompt": "When you face an unfamiliar problem with incomplete instructions, which starting point feels more natural?",
      "options": [
        {
          "optionId": "TB-SN-01",
          "text": "I first gather concrete facts, examples, and what has worked before, then build the solution from there.",
          "resolvedPole": "S"
        },
        {
          "optionId": "TB-SN-02",
          "text": "I first form an overall picture of the patterns and possibilities, then use specific details to test or refine it.",
          "resolvedPole": "N"
        }
      ]
    },
    {
      "questionId": "TB-TF-1",
      "dimension": "TF",
      "instruction": "Both options may describe you in different situations. If you had to choose, select the one that feels more natural to you most of the time.",
      "prompt": "When two solutions are both reasonable but you must choose one, which consideration is more likely to guide your final decision?",
      "options": [
        {
          "optionId": "TB-TF-01",
          "text": "I prefer the option supported by the most consistent criteria and defensible trade-offs, even if not everyone likes the outcome.",
          "resolvedPole": "T"
        },
        {
          "optionId": "TB-TF-02",
          "text": "I prefer the option that best accounts for the people involved and preserves trust, even if the rule is applied less uniformly.",
          "resolvedPole": "F"
        }
      ]
    },
    {
      "questionId": "TB-JP-1",
      "dimension": "JP",
      "instruction": "Both options may describe you in different situations. If you had to choose, select the one that feels more natural to you most of the time.",
      "prompt": "You begin an important project whose requirements may still change. Which approach feels more comfortable?",
      "options": [
        {
          "optionId": "TB-JP-01",
          "text": "I establish a provisional plan, milestones, and next steps early, then revise them if new information appears.",
          "resolvedPole": "J"
        },
        {
          "optionId": "TB-JP-02",
          "text": "I keep the structure relatively open at first, gather more information, and commit to details when they become necessary.",
          "resolvedPole": "P"
        }
      ]
    }
  ],

           "questionnaire": {
    "answerScale": [
      {
        "value": 1,
           "label": "Strongly Disagree"
      },
           {
           "value": 2,
           "label": "Disagree"
      },
           {
           "value": 3,
           "label": "Neither Agree nor Disagree"
      },
           {
           "value": 4,
           "label": "Agree"
      },
           {
           "value": 5,
           "label": "Strongly Agree"
      }
    ],

           "questions": [
      {
        "questionId": "Q1",
           "position": 1,
           "prompt": "I often understand my thoughts better by discussing them with other people.",
           "dimension": "EI",
           "keyedPole": "E"
      },
           {
           "questionId": "Q2",
           "position": 2,
           "prompt": "When learning something new, I prefer concrete examples before abstract explanations.",
           "dimension": "SN",
           "keyedPole": "S"
      },
           {
           "questionId": "Q3",
           "position": 3,
           "prompt": "When making a difficult decision, I first look for the most logically consistent option.",
           "dimension": "TF",
           "keyedPole": "T"
      },
           {
           "questionId": "Q4",
           "position": 4,
           "prompt": "I feel more comfortable when important plans are settled in advance.",
           "dimension": "JP",
           "keyedPole": "J"
      },
           {
           "questionId": "Q5",
           "position": 5,
           "prompt": "After spending a lot of time with people, I usually want some time alone to recharge.",
           "dimension": "EI",
           "keyedPole": "I"
      },
           {
           "questionId": "Q6",
           "position": 6,
           "prompt": "I enjoy looking for patterns and meanings that are not immediately obvious.",
           "dimension": "SN",
           "keyedPole": "N"
      },
           {
           "questionId": "Q7",
           "position": 7,
           "prompt": "I naturally consider how a decision will affect the people involved.",
           "dimension": "TF",
           "keyedPole": "F"
      },
           {
           "questionId": "Q8",
           "position": 8,
           "prompt": "I like leaving room to change my plans when new possibilities appear.",
           "dimension": "JP",
           "keyedPole": "P"
      },
           {
           "questionId": "Q9",
           "position": 9,
           "prompt": "Talking through an idea often helps me develop it.",
           "dimension": "EI",
           "keyedPole": "E"
      },
           {
           "questionId": "Q10",
           "position": 10,
           "prompt": "Clear instructions and specific examples help me feel confident about a task.",
           "dimension": "SN",
           "keyedPole": "S"
      },
           {
           "questionId": "Q11",
           "position": 11,
           "prompt": "In a disagreement, I tend to examine whether each argument is internally consistent.",
           "dimension": "TF",
           "keyedPole": "T"
      },
           {
           "questionId": "Q12",
           "position": 12,
           "prompt": "I usually prefer finishing important tasks before turning to less urgent ones.",
           "dimension": "JP",
           "keyedPole": "J"
      },
           {
           "questionId": "Q13",
           "position": 13,
           "prompt": "In a new group, I usually take some time to observe before becoming actively involved.",
           "dimension": "EI",
           "keyedPole": "I"
      },
           {
           "questionId": "Q14",
           "position": 14,
           "prompt": "I often find myself thinking about how things could develop in the future.",
           "dimension": "SN",
           "keyedPole": "N"
      },
           {
           "questionId": "Q15",
           "position": 15,
           "prompt": "I adjust the way I communicate when I know someone may be emotionally affected.",
           "dimension": "TF",
           "keyedPole": "F"
      },
           {
           "questionId": "Q16",
           "position": 16,
           "prompt": "I enjoy keeping several options open until a decision is really necessary.",
           "dimension": "JP",
           "keyedPole": "P"
      },
           {
           "questionId": "Q17",
           "position": 17,
           "prompt": "I enjoy being actively involved in conversations with several people.",
           "dimension": "EI",
           "keyedPole": "E"
      },
           {
           "questionId": "Q18",
           "position": 18,
           "prompt": "When solving a problem, I usually start with the facts that are directly available.",
           "dimension": "SN",
           "keyedPole": "S"
      },
           {
           "questionId": "Q19",
           "position": 19,
           "prompt": "I prefer feedback to be clear and precise, even when it may be uncomfortable to hear.",
           "dimension": "TF",
           "keyedPole": "T"
      },
           {
           "questionId": "Q20",
           "position": 20,
           "prompt": "Before starting a complicated project, I like to decide what the next steps will be.",
           "dimension": "JP",
           "keyedPole": "J"
      },
           {
           "questionId": "Q21",
           "position": 21,
           "prompt": "I usually prefer a few substantial conversations to many brief interactions.",
           "dimension": "EI",
           "keyedPole": "I"
      },
           {
           "questionId": "Q22",
           "position": 22,
           "prompt": "Abstract ideas can interest me even before I know whether they have a practical use.",
           "dimension": "SN",
           "keyedPole": "N"
      },
           {
           "questionId": "Q23",
           "position": 23,
           "prompt": "Preserving trust between people can matter as much to me as finding the most efficient solution.",
           "dimension": "TF",
           "keyedPole": "F"
      },
           {
           "questionId": "Q24",
           "position": 24,
           "prompt": "I adapt easily when a schedule changes unexpectedly.",
           "dimension": "JP",
           "keyedPole": "P"
      },
           {
           "questionId": "Q25",
           "position": 25,
           "prompt": "When I have free time, I often look for activities I can share with other people.",
           "dimension": "EI",
           "keyedPole": "E"
      },
           {
           "questionId": "Q26",
           "position": 26,
           "prompt": "I tend to notice practical details that affect whether an idea will actually work.",
           "dimension": "SN",
           "keyedPole": "S"
      },
           {
           "questionId": "Q27",
           "position": 27,
           "prompt": "I can usually separate my personal feelings from my evaluation of an argument.",
           "dimension": "TF",
           "keyedPole": "T"
      },
           {
           "questionId": "Q28",
           "position": 28,
           "prompt": "Having a clear endpoint for a task helps me work more comfortably.",
           "dimension": "JP",
           "keyedPole": "J"
      },
           {
           "questionId": "Q29",
           "position": 29,
           "prompt": "I often need private thinking time before I know what I really think about something.",
           "dimension": "EI",
           "keyedPole": "I"
      },
           {
           "questionId": "Q30",
           "position": 30,
           "prompt": "I enjoy making connections between ideas from very different subjects.",
           "dimension": "SN",
           "keyedPole": "N"
      },
           {
           "questionId": "Q31",
           "position": 31,
           "prompt": "When resolving a disagreement, maintaining the relationship is an important part of the solution for me.",
           "dimension": "TF",
           "keyedPole": "F"
      },
           {
           "questionId": "Q32",
           "position": 32,
           "prompt": "I enjoy discovering the direction of a project while I am working on it.",
           "dimension": "JP",
           "keyedPole": "P"
      },
           {
           "questionId": "Q33",
           "position": 33,
           "prompt": "In group discussions, contributing my thoughts aloud feels natural to me.",
           "dimension": "EI",
           "keyedPole": "E"
      },
           {
           "questionId": "Q34",
           "position": 34,
           "prompt": "When a familiar method works well, I usually see value in using it again.",
           "dimension": "SN",
           "keyedPole": "S"
      },
           {
           "questionId": "Q35",
           "position": 35,
           "prompt": "When several choices seem reasonable, comparing their advantages and disadvantages helps me decide.",
           "dimension": "TF",
           "keyedPole": "T"
      },
           {
           "questionId": "Q36",
           "position": 36,
           "prompt": "Trips or events feel easier to enjoy when the important details have been arranged beforehand.",
           "dimension": "JP",
           "keyedPole": "J"
      },
           {
           "questionId": "Q37",
           "position": 37,
           "prompt": "Even enjoyable social activities can leave me wanting a period of quiet afterward.",
           "dimension": "EI",
           "keyedPole": "I"
      },
           {
           "questionId": "Q38",
           "position": 38,
           "prompt": "I am often interested in trying a new approach simply because it might reveal a better possibility.",
           "dimension": "SN",
           "keyedPole": "N"
      },
           {
           "questionId": "Q39",
           "position": 39,
           "prompt": "When judging a decision, I pay close attention to whether the people involved are being treated fairly.",
           "dimension": "TF",
           "keyedPole": "F"
      },
           {
           "questionId": "Q40",
           "position": 40,
           "prompt": "I prefer adjusting my approach as I go rather than following a detailed plan from beginning to end.",
           "dimension": "JP",
           "keyedPole": "P"
      },
           {
           "questionId": "Q41",
           "position": 41,
           "prompt": "Sharing an experience with someone often helps me make sense of it.",
           "dimension": "EI",
           "keyedPole": "E"
      },
           {
           "questionId": "Q42",
           "position": 42,
           "prompt": "I usually want to understand how an idea works in practice before relying on it.",
           "dimension": "SN",
           "keyedPole": "S"
      },
           {
           "questionId": "Q43",
           "position": 43,
           "prompt": "When a problem becomes stressful, my first instinct is usually to identify what can be fixed.",
           "dimension": "TF",
           "keyedPole": "T"
      },
           {
           "questionId": "Q44",
           "position": 44,
           "prompt": "Unresolved important decisions tend to stay on my mind until I settle them.",
           "dimension": "JP",
           "keyedPole": "J"
      },
           {
           "questionId": "Q45",
           "position": 45,
           "prompt": "I am comfortable spending long stretches of time working independently.",
           "dimension": "EI",
           "keyedPole": "I"
      },
           {
           "questionId": "Q46",
           "position": 46,
           "prompt": "Possibilities and underlying concepts often capture my attention more than concrete details do.",
           "dimension": "SN",
           "keyedPole": "N"
      },
           {
           "questionId": "Q47",
           "position": 47,
           "prompt": "When someone tells me about a problem, my first instinct is often to understand how they feel about it.",
           "dimension": "TF",
           "keyedPole": "F"
      },
           {
           "questionId": "Q48",
           "position": 48,
           "prompt": "I feel most comfortable when plans leave some room for spontaneity.",
           "dimension": "JP",
           "keyedPole": "P"
      }
    ]
  },

           "scoringPolicy": {
    "type": "CENTERED_BALANCED_KEYING",
           "revision": "v1",
           "answerCenter": 3,
           "minimumAnswer": 1,
           "maximumAnswer": 5,
           "itemsPerDimension": 12,
           "maximumAbsoluteRawScore": 24
  },

           "ambiguityPolicy": {
    "type": "ABSOLUTE_RAW_SCORE_THRESHOLD",
           "revision": "v1",
           "inclusiveThreshold": 2
  },

           "clarificationPolicy": {
    "type": "DIMENSION_SCOPED_AI_CLARIFICATION",
           "revision": "v1"
  },

           "finalizationPolicy": {
    "type": "QUESTIONNAIRE_WITH_OPTIONAL_CLARIFICATION",
           "revision": "v2"
  }
}
$spec$::jsonb,
           TIMESTAMPTZ '2026-10-05 00:00:00+00',
           NULL
       );
