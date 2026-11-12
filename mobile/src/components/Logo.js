import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { theme } from '../theme';

const { colors, fonts } = theme;

// Logo iz Figme (teg sa natpisom "pumpd"), nacrtan od View elemenata.
// Zašto ne slika: nema pozadine oko tega, oštar je na svakoj veličini
// i ne treba novi paket (react-native-svg) ni PNG u 2x/3x varijantama.
//
// Sve mjere su odnos prema visini tega (`height`), izmjerene iz Figme:
// tegovi 140 px visine, ukupna širina 400 px.
const R = {
  plateW: 45 / 140,
  plateRadius: 8 / 140,
  stepW: 17 / 140,
  stepH: 90 / 140,
  endW: 8 / 140,
  barH: 45 / 140,
  barW: 260 / 140,
  fontSize: 26 / 140,
};

export const LOGO_ASPECT_RATIO = 400 / 140;

export const Logo = ({ height = 44, color = colors.primary, textColor = colors.background }) => {
  const h = (ratio) => Math.round(ratio * height * 10) / 10;

  const piece = (w, ht, radius = h(R.plateRadius) / 2) => ({
    width: h(w),
    height: h(ht),
    backgroundColor: color,
    borderRadius: radius,
  });

  return (
    <View
      style={styles.row}
      accessible
      accessibilityRole="image"
      accessibilityLabel="pumpd"
    >
      <View style={piece(R.endW, R.barH, 1)} />
      <View style={piece(R.stepW, R.stepH)} />
      <View style={{ ...piece(R.plateW, 1, h(R.plateRadius)), height }} />

      <View style={[piece(R.barW, R.barH, 0), styles.bar]}>
        <Text
          style={[styles.text, { color: textColor, fontSize: h(R.fontSize) }]}
          allowFontScaling={false}
          numberOfLines={1}
        >
          pumpd
        </Text>
      </View>

      <View style={{ ...piece(R.plateW, 1, h(R.plateRadius)), height }} />
      <View style={piece(R.stepW, R.stepH)} />
      <View style={piece(R.endW, R.barH, 1)} />
    </View>
  );
};

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  bar: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  text: {
    fontFamily: fonts.spartan600,
    includeFontPadding: false,
  },
});
