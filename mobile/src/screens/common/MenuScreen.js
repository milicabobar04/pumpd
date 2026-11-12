import React from 'react';
import { View, ScrollView, StyleSheet } from 'react-native';
import { Ionicons, MaterialCommunityIcons } from '@expo/vector-icons';
import { Button } from '../../components';
import { theme } from '../../theme';

const { colors, spacing, borderRadius } = theme;

const ICON_SETS = { ion: Ionicons, mci: MaterialCommunityIcons };
const ICON_SIZE = 64;
const BOX_SIZE = 72;

// Početni ekran radnika i administratora iz Figme:
// velika zelena ikona, ispod nje dugme koje otvara dio aplikacije.
//
// items: [{ label, screen, icon, iconSet: 'ion' | 'mci', boxed }]
//   screen - ime ekrana u istom stacku (navigation.navigate(screen))
//   boxed  - ikona u tamnom kvadratu (kao "Uređivanje informacija" u Figmi)
export default function MenuScreen({ navigation, items }) {
  return (
    <ScrollView
      style={styles.scroll}
      contentContainerStyle={styles.content}
    >
      {items.map((item) => {
        const Icon = ICON_SETS[item.iconSet ?? 'ion'];
        const icon = (
          <Icon
            name={item.icon}
            size={item.boxed ? ICON_SIZE * 0.7 : ICON_SIZE}
            color={colors.primary}
          />
        );

        return (
          <View key={item.screen} style={styles.item}>
            {/* Ikona je ukras; čitač ekrana čita natpis na dugmetu */}
            <View
              style={item.boxed ? styles.box : styles.iconWrap}
              accessibilityElementsHidden
              importantForAccessibility="no-hide-descendants"
            >
              {icon}
            </View>
            <Button
              title={item.label}
              variant="menu"
              onPress={() => navigation.navigate(item.screen)}
            />
          </View>
        );
      })}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  scroll: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    flexGrow: 1,
    justifyContent: 'space-evenly',
    alignItems: 'center',
    paddingHorizontal: spacing.px24,
    paddingVertical: spacing.px32,
    gap: spacing.px32,
  },
  item: {
    width: '100%',
    alignItems: 'center',
    gap: spacing.px16,
  },
  iconWrap: {
    height: BOX_SIZE,
    justifyContent: 'center',
  },
  box: {
    width: BOX_SIZE,
    height: BOX_SIZE,
    borderRadius: borderRadius.px8,
    backgroundColor: colors.surface,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
