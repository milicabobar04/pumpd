import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { useAuth } from '../../context/AuthContext';
import { theme } from '../../theme';

// privremeni profil (pravi ekran 2.1 radi F2); odjava je u headeru
export default function ProfileScreen() {
  const { user } = useAuth();

  return (
    <View style={styles.container}>
      <Text style={theme.typography.screenTitle}>
        {user?.name} {user?.surname}
      </Text>
      <Text style={theme.typography.caption}>
        {user?.email} · {user?.role}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: theme.colors.background,
    alignItems: 'center',
    justifyContent: 'center',
    padding: theme.spacing.px24,
    gap: theme.spacing.px16,
  },
});