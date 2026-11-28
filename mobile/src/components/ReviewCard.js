import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { theme, cards } from '../theme';
import { formatDate } from '../utils/date';
import { StarRating } from './StarRating';

const { colors, spacing } = theme;
const MAX_FONT_SCALE = 1.3;

// Jedna recenzija trenera: ime člana i datum, zvjezdice, pa komentar (ako postoji).
//
// review: { reviewId, memberName, grade, comment, date }  (shema Review iz ugovora)
//
// <ReviewCard review={recenzija} />
export const ReviewCard = ({ review, style }) => (
  <View style={[styles.review, style]}>
    <View style={styles.reviewHeader}>
      <Text style={[cards.review.title, styles.reviewName]} numberOfLines={1} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {review.memberName}
      </Text>
      <Text style={cards.review.body} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {formatDate(review.date)}
      </Text>
    </View>
    <StarRating value={review.grade} size={18} color={colors.background} emptyColor={colors.gray500} />
    {review.comment?.trim() ? (
      <Text style={cards.review.body} maxFontSizeMultiplier={MAX_FONT_SCALE}>
        {review.comment}
      </Text>
    ) : null}
  </View>
);

const styles = StyleSheet.create({
  review: { ...cards.review.container, gap: spacing.px6 },
  reviewHeader: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: spacing.px8 },
  reviewName: { flex: 1 },
});

export default ReviewCard;
