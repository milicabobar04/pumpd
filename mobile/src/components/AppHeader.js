import React, { useState } from 'react';
import { View, Pressable, StyleSheet } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { Ionicons } from '@expo/vector-icons';
import { Logo } from './Logo';
import { ConfirmDialog } from './ConfirmDialog';
import { useAuth } from '../context/AuthContext';
import { theme } from '../theme';

const { colors, spacing } = theme;

const ICON_SIZE = 24;
const HIT_SLOP = { top: 12, bottom: 12, left: 8, right: 8 }; // dodir najmanje ~48 px

// Zvonce (obavještenja, 6.1) prema Figmi: član i trener.
// Radnik i administrator ga nemaju. Koristi ga i RootNavigator.
const ROLES_WITH_NOTIFICATIONS = ['MEMBER', 'TRAINER'];
export const hasNotifications = (role) => ROLES_WITH_NOTIFICATIONS.includes(role);

const HeaderIcon = ({ name, label, onPress }) => (
  <Pressable
    onPress={onPress}
    hitSlop={HIT_SLOP}
    accessibilityRole="button"
    accessibilityLabel={label}
    style={({ pressed }) => pressed && styles.pressed}
  >
    <Ionicons name={name} size={ICON_SIZE} color={colors.primary} />
  </Pressable>
);

// Ikona za odjavu iz Figme: zeleni krug sa tamnom strelicom.
const LogoutIcon = ({ onPress }) => (
  <Pressable
    onPress={onPress}
    hitSlop={HIT_SLOP}
    accessibilityRole="button"
    accessibilityLabel="Odjava"
    style={({ pressed }) => [styles.logout, pressed && styles.pressed]}
  >
    <Ionicons name="log-out-outline" size={16} color={colors.background} />
  </Pressable>
);

// Zajednički header iz Figme.
//   Član, trener:      [nazad] [odjava] — logo — [zvonce] [zupčanik]
//   Radnik, admin:     [nazad]          — logo — [odjava] [zupčanik]
// Odjava ide lijevo kad desno ima zvonce, inače desno (kao u Figmi).
//
// navigation: dobija se od React Navigation (header: ({ navigation }) => ...)
// showBack:   strelica nazad, samo kad ima kuda nazad (ne na početnom ekranu uloge)
export const AppHeader = ({ navigation, showBack = false }) => {
  const insets = useSafeAreaInsets();
  const { role, signOut } = useAuth();
  const [logoutOpen, setLogoutOpen] = useState(false);

  const withBell = hasNotifications(role);
  const logout = <LogoutIcon onPress={() => setLogoutOpen(true)} />;

  // Faza 1: prije signOut() pozvati POST /auth/logout (1.2), uz loading na dugmetu.
  const confirmLogout = () => {
    setLogoutOpen(false);
    signOut();
  };

  return (
    <View style={[styles.container, { paddingTop: insets.top + spacing.px8 }]}>
      {/* Lijeva i desna strana imaju istu širinu (flex: 1), pa je logo uvijek tačno u sredini */}
      <View style={styles.side}>
        {showBack ? (
          <HeaderIcon name="arrow-back" label="Nazad" onPress={() => navigation.goBack()} />
        ) : null}
        {withBell ? logout : null}
      </View>

      <Logo height={36} />

      <View style={[styles.side, styles.right]}>
        {withBell ? (
          <HeaderIcon
            name="notifications-outline"
            label="Obavještenja"
            onPress={() => navigation.navigate('Notifications')}
          />
        ) : (
          logout
        )}
        <HeaderIcon
          name="settings-outline"
          label="Podešavanja naloga"
          onPress={() => navigation.navigate('Settings')}
        />
      </View>

      <ConfirmDialog
        visible={logoutOpen}
        title="Odjava"
        message="Da li ste sigurni da želite da se odjavite?"
        confirmLabel="Odjavi se"
        confirmVariant="danger"
        onConfirm={confirmLogout}
        onCancel={() => setLogoutOpen(false)}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingHorizontal: spacing.px16,
    paddingBottom: spacing.px8,
    backgroundColor: colors.background,
    // zelena linija ispod headera iz Figme
    borderBottomWidth: 1,
    borderBottomColor: colors.primary,
  },
  side: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.px16,
  },
  right: {
    justifyContent: 'flex-end',
  },
  logout: {
    width: ICON_SIZE,
    height: ICON_SIZE,
    borderRadius: ICON_SIZE / 2,
    backgroundColor: colors.primary,
    alignItems: 'center',
    justifyContent: 'center',
  },
  pressed: {
    opacity: 0.6,
  },
});
