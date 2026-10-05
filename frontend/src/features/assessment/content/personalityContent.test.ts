import {
  describe,
  expect,
  it,
} from 'vitest'

import {
  getPersonalityLetterDescriptions,
  getPersonalityTypeDescription,
  personalityLetterDescriptions,
  personalityTypeDescriptions,
  type PersonalityTypeCode,
} from './personalityContent'
import { getPersonalityImagePath } from './personalityImages'

const expectedTypes: PersonalityTypeCode[] = [
  'ISTJ',
  'ISFJ',
  'INFJ',
  'INTJ',
  'ISTP',
  'ISFP',
  'INFP',
  'INTP',
  'ESTP',
  'ESFP',
  'ENFP',
  'ENTP',
  'ESTJ',
  'ESFJ',
  'ENFJ',
  'ENTJ',
]

describe('personality content catalog', () => {
  it('contains all eight preference letters', () => {
    expect(
      Object.keys(
        personalityLetterDescriptions,
      ).sort(),
    ).toEqual(
      [
        'E',
        'F',
        'I',
        'J',
        'N',
        'P',
        'S',
        'T',
      ],
    )
  })

  it('contains all sixteen supported final types', () => {
    expect(
      Object.keys(
        personalityTypeDescriptions,
      ).sort(),
    ).toEqual(
      [...expectedTypes].sort(),
    )
  })

  it('resolves every type into four letter descriptions and one stable image path', () => {
    for (const typeCode of expectedTypes) {
      expect(
        getPersonalityTypeDescription(
          typeCode,
        ),
      ).not.toBeNull()

      expect(
        getPersonalityLetterDescriptions(
          typeCode,
        ).map(
          (description) =>
            description.letter,
        ),
      ).toEqual(
        typeCode.split(''),
      )

      expect(
        getPersonalityImagePath(
          typeCode,
        ),
      ).toBe(
        `/personality/${typeCode.toLowerCase()}.png`,
      )
    }
  })
})
