import { theme } from './theme';

const { colors, fonts, spacing, borderRadius } = theme;

// ---------------------------------------------------------------------
// Osnovni stilovi koje kartice proširuju
// ---------------------------------------------------------------------

const rowContainer = {
  backgroundColor: colors.surface,
  borderRadius: borderRadius.px10,
};

const rowTitle = {
  fontFamily: fonts.montserrat600,
  fontSize: 20,
  color: colors.textLight,
};

const rowSubtitle = {
  fontFamily: fonts.montserrat500,
  fontSize: 18,
  color: colors.textWorkoutDetails,
};

const thumbnail = {
  width: 52,
  height: 52,
  borderRadius: borderRadius.px10,
};

const translucentContainer = {
  backgroundColor: colors.white88,
  borderRadius: borderRadius.px20,
  paddingVertical: spacing.px14,
  paddingHorizontal: spacing.px16,
};

const translucentTitle = {
  fontFamily: fonts.roboto400,
  fontSize: 15,
  lineHeight: 20,
  color: colors.gray900,
};

const translucentBody = {
  fontFamily: fonts.roboto400,
  fontSize: 13,
  lineHeight: 20,
  color: colors.gray600,
};

const outlinedContainer = {
  backgroundColor: colors.surface,
  borderColor: colors.border,
  borderWidth: 1,
  borderRadius: borderRadius.px15,
};

const personLabel = {
  fontFamily: fonts.actor400,
  fontSize: 18,
  color: colors.border,
};

// ---------------------------------------------------------------------
// Kartice
// ---------------------------------------------------------------------

export const cards = {
  // Vježba — standardna varijanta
  exercise: {
    container: { ...rowContainer },
    thumbnail: { ...thumbnail },
    title: { ...rowTitle },
    subtitle: { ...rowSubtitle },
  },

  // Vježba — zelena varijanta
  exerciseSelected: {
    container: {
      ...rowContainer,
      backgroundColor: colors.primary,
    },
    thumbnail: { ...thumbnail },
    title: {
      ...rowTitle,
      color: colors.background,
    },
    subtitle: {
      ...rowSubtitle,
      color: colors.background,
    },
  },

  // Smeđa varijanta iz dizajna.
  // Nije pretpostavljeno da predstavlja disabled stanje.
  exerciseBrown: {
    container: {
      ...rowContainer,
      backgroundColor: colors.surfaceBrown,
    },
    thumbnail: { ...thumbnail },
    title: { ...rowTitle },
    subtitle: { ...rowSubtitle },
  },

  // Grupa: naziv + termin
  trainingGroup: {
    container: { ...rowContainer },
    thumbnail: { ...thumbnail },
    title: { ...rowTitle },
    subtitle: { ...rowSubtitle },
  },

  // Zahtjev za praćenje
  followRequest: {
    container: { ...translucentContainer },
    content: { gap: spacing.px12 },
    textGroup: { gap: spacing.px4 },
    title: { ...translucentTitle },
    body: { ...translucentBody },
  },

  // Recenzija trenera
  review: {
    container: { ...translucentContainer },
    textGroup: { gap: spacing.px4 },
    title: { ...translucentTitle },
    body: { ...translucentBody },
    rating: {
      flexDirection: 'row',
      gap: spacing.px4,
    },
    star: {
      width: 18,
      height: 17.25,
    },
  },

  // Zajednički stil za članove, trenere i radnike.
  // Avatar se nalazi izvan obrubljenog polja.
  person: {
    container: { ...outlinedContainer },
    label: { ...personLabel },
    avatar: {
      width: 36,
      height: 34,
    },
  },

  // Red sa prijavljenim problemom
  issue: {
    container: { ...outlinedContainer },
    label: { ...personLabel },
    icon: {
      width: 34,
      height: 34,
    },
  },

  // Datum uplate, datum isteka ili cijena članarine
  membership: {
    container: { ...outlinedContainer },
    value: {
      ...personLabel,
      fontSize: 22,
    },
    label: {
      fontFamily: fonts.actor400,
      fontSize: 20,
      color: colors.textPrimary,
    },
  },

  // Aktivnost u rasporedu
  scheduleActivity: {
    container: {
      backgroundColor: colors.surface,
      borderColor: colors.primary,
      borderWidth: 1,
      borderRadius: borderRadius.px14,
    },
    title: {
      fontFamily: fonts.inter600,
      fontSize: 20,
      letterSpacing: 0.3,
      color: colors.textPrimary,
    },
    closeIcon: {
      width: 24,
      height: 24,
    },
  },

  // Objava: odvojeni elementi bez posebne pozadine kartice.
  // U Figmi je SF Pro (Appleov font, ne smije se ugraditi), pa se koristi Inter.
  post: {
    container: {
      backgroundColor: 'transparent',
    },
    author: {
      fontFamily: fonts.inter500,
      fontSize: 15,
      color: colors.textPrimary,
    },
    timestamp: {
      fontFamily: fonts.inter400,
      fontSize: 13,
      color: colors.textMuted,
    },
    body: {
      fontFamily: fonts.inter600,
      fontSize: 20,
      lineHeight: 29.620800018310546,
      color: colors.textPrimary,
    },
    image: {
      width: 385,
      height: 291,
      borderRadius: borderRadius.none,
    },
  },

  // Težina, godine i visina
  profileStats: {
    container: {
      backgroundColor: colors.background,
      borderRadius: borderRadius.px10,
    },
    value: {
      fontFamily: fonts.spartan600,
      fontSize: 15,
      color: colors.textPrimary,
    },
    label: {
      fontFamily: fonts.spartan300,
      fontSize: 15,
      color: colors.textPrimary,
    },
    divider: {
      width: 1,
      height: 41.5,
      backgroundColor: colors.textPrimary,
    },
  },

  // Panel forme za prijavu
  loginPanel: {
    container: {
      backgroundColor: colors.background,
      borderColor: colors.border,
      borderWidth: 1,
      borderRadius: borderRadius.px8,
      padding: spacing.px24,
      gap: spacing.px24,
    },
  },

  // Podloga QR prikaza
  qrPanel: {
    container: {
      backgroundColor: colors.primary,
      borderRadius: borderRadius.none,
    },
    image: {
      width: 223,
      height: 223,
    },
  },
};

// Dimenzije konkretnih primjera, ne univerzalni minimumi.
// Kod person/issue, container je samo polje uz ikonu ili avatar.
export const cardSizes = {
  exercise: { width: 346, height: 66 },
  trainingGroup: { width: 346, height: 66 },
  followRequest: { width: 340, height: 100 },
  review: { width: 366, height: 72 },
  person: { width: 243, height: 45 },
  personRow: { width: 308, height: 45 },
  issue: { width: 243, height: 45 },
  membership: { width: 318, height: 47 },
  membershipProfile: { width: 292, height: 47 },
  scheduleActivity: { width: 242, height: 64 },
  profileStats: { width: 323, height: 60 },
  loginPanel: { width: 320, height: 337 },
  qrPanel: { width: 250, height: 250 },
};

// Dodatni efekti iz dizajna — nisu direktni React Native stilovi.
// Za njih je potrebna komponenta za zamućenje pozadine.
export const cardEffects = {
  followRequest: { backgroundBlurRadius: 10 },
  review: { backgroundBlurRadius: 10 },
};

export default cards;
