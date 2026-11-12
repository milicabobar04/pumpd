import React from 'react';
import { View, Text, Image, Pressable, StyleSheet } from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import { theme, cards, cardSizes } from '../theme';

const { colors, spacing } = theme;
const MAX_FONT_SCALE = 1.3;

// Varijante sa sličicom, naslovom i podnaslovom unutar kartice
const ROW_VARIANTS = ['exercise', 'exerciseSelected', 'exerciseBrown', 'trainingGroup'];

// Ikona kad sličica nije zadata
const DEFAULT_ICONS = {
  exercise: 'barbell-outline',
  exerciseSelected: 'barbell-outline',
  exerciseBrown: 'barbell-outline',
  trainingGroup: 'people-outline',
  person: 'person-circle-outline',
  issue: 'alert-circle-outline',
};

// Red liste nad stilovima iz theme/Cards.js.
//
// variant:
//   'person'        član, trener ili radnik (avatar lijevo, izvan obrubljenog polja)
//   'issue'         prijavljeni problem (ikona lijevo, izvan polja)
//   'exercise' | 'exerciseSelected' | 'exerciseBrown'   vježba
//   'trainingGroup' grupa: naziv i termin
//
// image:  izvor slike, npr. { uri: korisnik.slikaUrl } ili require('...')
// icon:   naziv Ionicons ikone, kad nema slike
// right:  bilo šta desno u redu (strelica, chip, broj)
// Kad je zadat onPress, red se može pritisnuti.
export const ListItem = ({
  variant = 'person',
  title,
  subtitle,
  image,
  icon,
  right,
  onPress,
  disabled = false,
  style,
  accessibilityLabel,
}) => {
  const card = cards[variant] ? cards[variant] : cards.person;
  const isRow = ROW_VARIANTS.includes(variant);
  const label = accessibilityLabel || [title, subtitle].filter(Boolean).join(', ');

  const content = isRow ? (
    <RowContent variant={variant} card={card} title={title} subtitle={subtitle} image={image} icon={icon} right={right} />
  ) : (
    <FieldContent variant={variant} card={card} title={title} subtitle={subtitle} image={image} icon={icon} right={right} />
  );

  const baseStyle = isRow
    ? [card.container, styles.row, { minHeight: rowHeight(variant) }, style]
    : [styles.fieldRow, style];

  if (!onPress) {
    return (
      <View style={baseStyle} accessible accessibilityLabel={label}>
        {content}
      </View>
    );
  }

  return (
    <Pressable
      onPress={onPress}
      disabled={disabled}
      style={({ pressed }) => [baseStyle, pressed && styles.pressed, disabled && styles.disabled]}
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled }}
    >
      {content}
    </Pressable>
  );
};

// Vježba ili grupa: sličica, pa naslov i podnaslov, sve u jednoj kartici
const RowContent = ({ variant, card, title, subtitle, image, icon, right }) => {
  const iconColor = variant === 'exerciseSelected' ? colors.background : colors.iconMuted;

  return (
    <>
      {image ? (
        <Image source={image} style={card.thumbnail} />
      ) : (
        <View style={[card.thumbnail, styles.thumbnailPlaceholder]}>
          <Ionicons name={icon || DEFAULT_ICONS[variant]} size={26} color={iconColor} />
        </View>
      )}

      <View style={styles.texts}>
        <Text style={card.title} numberOfLines={1} maxFontSizeMultiplier={MAX_FONT_SCALE}>
          {title}
        </Text>
        {subtitle ? (
          <Text style={card.subtitle} numberOfLines={1} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {subtitle}
          </Text>
        ) : null}
      </View>

      {right}
    </>
  );
};

// Osoba ili problem: avatar/ikona izvan obrubljenog polja, tekst u polju
const FieldContent = ({ variant, card, title, subtitle, image, icon, right }) => {
  const leadingSize = variant === 'issue' ? card.icon : card.avatar;

  return (
    <>
      {image ? (
        <Image source={image} style={[leadingSize, styles.avatarImage]} />
      ) : (
        <Ionicons name={icon || DEFAULT_ICONS[variant]} size={34} color={colors.iconMuted} />
      )}

      <View style={[card.container, styles.field, { minHeight: cardSizes[variant]?.height ?? 45 }]}>
        <View style={styles.texts}>
          <Text style={card.label} numberOfLines={1} maxFontSizeMultiplier={MAX_FONT_SCALE}>
            {title}
          </Text>
          {subtitle ? (
            <Text style={[card.label, styles.fieldSubtitle]} numberOfLines={1} maxFontSizeMultiplier={MAX_FONT_SCALE}>
              {subtitle}
            </Text>
          ) : null}
        </View>
        {right}
      </View>
    </>
  );
};

// exerciseSelected i exerciseBrown nemaju svoju veličinu, koriste veličinu vježbe
const rowHeight = (variant) =>
  (cardSizes[variant] || cardSizes.exercise).height;

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.px12,
    padding: spacing.px7, // 52 px sličica + 2 x 7 = 66 px kao u Figmi
    paddingRight: spacing.px16,
  },
  fieldRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.px12,
  },
  field: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.px8,
    paddingHorizontal: spacing.px16,
    paddingVertical: spacing.px6,
  },
  texts: {
    flex: 1,
    gap: spacing.px2,
  },
  fieldSubtitle: {
    fontSize: 14,
    color: colors.textSecondary,
  },
  thumbnailPlaceholder: {
    backgroundColor: colors.surfaceSlate,
    alignItems: 'center',
    justifyContent: 'center',
  },
  avatarImage: {
    borderRadius: 999,
  },
  pressed: {
    opacity: 0.8,
  },
  disabled: {
    opacity: 0.6,
  },
});

export default ListItem;
