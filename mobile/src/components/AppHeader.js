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
//   Član:              [nazad] [zvonce] [QR] — logo — [zupčanik] [odjava]
//   Trener:            [nazad] [zvonce]      — logo — [zupčanik] [odjava]
//   Radnik, admin:     [nazad]               — logo — [zupčanik] [odjava]
//   Promjena lozinke:                        — logo —            [odjava]
// Navigacija i prečice su lijevo, nalog (podešavanja, odjava) uvijek desno.
//
// navigation: dobija se od React Navigation (header: ({ navigation }) => ...)
// showBack:   strelica nazad, samo kad ima kuda nazad (ne na početnom ekranu uloge)
// minimal:    samo logo i odjava (ekran obavezne promjene privremene lozinke,
//             gdje ne postoje ekrani Obavještenja i Podešavanja)
export const AppHeader = ({ navigation, showBack = false, minimal = false }) => {
  const insets = useSafeAreaInsets();
  const { role, logout: logoutUser } = useAuth();
  const [logoutOpen, setLogoutOpen] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  const withBell = !minimal && hasNotifications(role);
  // Član na svakom ekranu ima prečicu do svog QR koda (ekran "Moja članarina", 2.3)
  const withQr = !minimal && role === 'MEMBER';
  const logout = <LogoutIcon onPress={() => setLogoutOpen(true)} />;

  // SU-04: POST /auth/logout (1.2), pa brisanje sesije sa uređaja.
  // Dijalog ostaje otvoren sa spinnerom dok poziv traje; kad se sesija obriše,
  // RootNavigator prikazuje ekran prijave i ovaj header nestaje.
  const confirmLogout = async () => {
    setLoggingOut(true);
    await logoutUser();
  };

  return (
    <View style={[styles.container, { paddingTop: insets.top + spacing.px8 }]}>
      {/* Lijeva i desna strana imaju istu širinu (flex: 1), pa je logo uvijek tačno u sredini */}
      <View style={styles.side}>
        {showBack ? (
          <HeaderIcon name="arrow-back" label="Nazad" onPress={() => navigation.goBack()} />
        ) : null}
        {withBell ? (
          <HeaderIcon
            name="notifications-outline"
            label="Obavještenja"
            onPress={() => navigation.navigate('Notifications')}
          />
        ) : null}
        {withQr ? (
          <HeaderIcon
            name="qr-code-outline"
            label="Moj QR kod"
            onPress={() => navigation.navigate('MyMembership')}
          />
        ) : null}
      </View>

      <Logo height={36} />

      <View style={[styles.side, styles.right]}>
        {minimal ? null : (
          <HeaderIcon
            name="settings-outline"
            label="Podešavanja naloga"
            onPress={() => navigation.navigate('Settings')}
          />
        )}
        {logout}
      </View>

      <ConfirmDialog
        visible={logoutOpen}
        title="Odjava"
        message="Da li ste sigurni da želite da se odjavite?"
        confirmLabel="Odjavi se"
        confirmVariant="danger"
        loading={loggingOut}
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
