import type { PersonalityTypeCode } from './personalityContent'

const personalityImagePaths: Record<
  PersonalityTypeCode,
  string
> = {
  ISTJ: '/personality/istj.png',
  ISFJ: '/personality/isfj.png',
  INFJ: '/personality/infj.png',
  INTJ: '/personality/intj.png',
  ISTP: '/personality/istp.png',
  ISFP: '/personality/isfp.png',
  INFP: '/personality/infp.png',
  INTP: '/personality/intp.png',
  ESTP: '/personality/estp.png',
  ESFP: '/personality/esfp.png',
  ENFP: '/personality/enfp.png',
  ENTP: '/personality/entp.png',
  ESTJ: '/personality/estj.png',
  ESFJ: '/personality/esfj.png',
  ENFJ: '/personality/enfj.png',
  ENTJ: '/personality/entj.png',
}

export function getPersonalityImagePath(
  typeCode: PersonalityTypeCode,
): string {
  return personalityImagePaths[typeCode]
}
