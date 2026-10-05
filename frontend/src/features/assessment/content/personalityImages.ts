import type { PersonalityTypeCode } from './personalityContent'

const personalityImagePaths: Record<
  PersonalityTypeCode,
  string
> = {
  ISTJ: '/personality/istj.webp',
  ISFJ: '/personality/isfj.webp',
  INFJ: '/personality/infj.webp',
  INTJ: '/personality/intj.webp',
  ISTP: '/personality/istp.webp',
  ISFP: '/personality/isfp.webp',
  INFP: '/personality/infp.webp',
  INTP: '/personality/intp.webp',
  ESTP: '/personality/estp.webp',
  ESFP: '/personality/esfp.webp',
  ENFP: '/personality/enfp.webp',
  ENTP: '/personality/entp.webp',
  ESTJ: '/personality/estj.webp',
  ESFJ: '/personality/esfj.webp',
  ENFJ: '/personality/enfj.webp',
  ENTJ: '/personality/entj.webp',
}

export function getPersonalityImagePath(
  typeCode: PersonalityTypeCode,
): string {
  return personalityImagePaths[typeCode]
}
