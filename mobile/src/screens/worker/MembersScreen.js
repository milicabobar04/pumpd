import React, { useCallback, useEffect, useRef, useState } from 'react';
import { View, Text, FlatList, Pressable, RefreshControl, ActivityIndicator, StyleSheet } from 'react-native';
import { useFocusEffect } from '@react-navigation/native';
import { Ionicons } from '@expo/vector-icons';
import { EmptyState, ErrorMessage, FormField, ListItem } from '../../components';
import { theme } from '../../theme';
import { getMembers } from '../../services/membersService';

const { colors, spacing, typography } = theme;
const MAX_FONT_SCALE = 1.3;
const SEARCH_DELAY_MS = 300;

const AddLink = ({ title, icon = 'add', onPress }) => (
  <Pressable
    onPress={onPress}
    hitSlop={12}
    style={({ pressed }) => [styles.addLink, pressed && styles.pressed]}
    accessibilityRole="button"
    accessibilityLabel={title}
  >
    <Text style={styles.addLinkText} maxFontSizeMultiplier={MAX_FONT_SCALE}>{title}</Text>
    <Ionicons name={icon} size={24} color={colors.primary} />
  </Pressable>
);

export default function MembersScreen({ navigation, route }) {
  const membershipMode = route.params?.mode === 'membership';
  const [members, setMembers] = useState(null); 
  const [search, setSearch] = useState('');
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [error, setError] = useState(null);

  const requestId = useRef(0);
  const searchRef = useRef(search);
  const firstRender = useRef(true);
  searchRef.current = search;

  const load = useCallback(async (text) => {
    const id = ++requestId.current;
    setError(null);
    try {
      const data = await getMembers(text.trim());
      if (id === requestId.current) setMembers(data);
    } catch (e) {
      if (id === requestId.current) setError(e);
    } finally {
      if (id === requestId.current) setLoading(false);
    }
  }, []);

  useFocusEffect(
    useCallback(() => {
      load(searchRef.current);
    }, [load]),
  );

  useEffect(() => {
    if (firstRender.current) {
      firstRender.current = false;
      return undefined;
    }
    const timer = setTimeout(() => load(search), SEARCH_DELAY_MS);
    return () => clearTimeout(timer);
  }, [search, load]);

  const refresh = async () => {
    setRefreshing(true);
    await load(search);
    setRefreshing(false);
  };

  const retry = () => {
    setLoading(true);
    load(search);
  };

  const header = (
    <View style={styles.header}>
        {membershipMode ? (
          <AddLink title="Skeniraj QR kod" icon="qr-code-outline" onPress={() => navigation.navigate('WorkerScanQr')} />
        ) : (
          <AddLink title="Kreiraj novog člana" onPress={() => navigation.navigate('WorkerAddMember')} />
        )}
        <FormField
        label="Pretraga"
        placeholder="Ime, prezime ili email"
        value={search}
        onChangeText={setSearch}
        autoCapitalize="none"
        autoCorrect={false}
        returnKeyType="search"
        />
        {error && members ? <ErrorMessage error={error} inline /> : null}
    </View>
    );

  if (loading && !members) {
    return (
      <View style={[styles.screen, styles.centered]}>
        <ActivityIndicator size="large" color={colors.primary} accessibilityLabel="Učitavanje članova" />
      </View>
    );
  }

  if (!members) {
    return (
      <View style={styles.screen}>
        <ErrorMessage error={error} onRetry={retry} />
      </View>
    );
  }

  return (
    <View style={styles.screen}>
      <FlatList
        data={members}
        keyExtractor={(item) => String(item.memberId)}
        renderItem={({ item }) => (
            <ListItem
                variant="person"
                icon="person-outline"
                title={`${item.name} ${item.surname}`}
                onPress={() =>
                  membershipMode
                    ? navigation.navigate('WorkerMemberMembership', {
                        memberId: item.memberId,
                        memberName: `${item.name} ${item.surname}`,
                      })
                    : navigation.navigate('WorkerMemberDetails', { memberId: item.memberId })
                }
            />
        )}
        ListHeaderComponent={header}
        ListEmptyComponent={
          <EmptyState
            icon="people-outline"
            title={search.trim() ? 'Nema rezultata' : 'Nema članova'}
            message={search.trim() ? 'Nijedan član ne odgovara pretrazi.' : 'Dodajte prvog člana.'}
          />
        }
        contentContainerStyle={styles.content}
        keyboardShouldPersistTaps="handled"
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={refresh}
            tintColor={colors.primary}
            colors={[colors.primary]}
            progressBackgroundColor={colors.surface}
          />
        }
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.background },
  centered: { alignItems: 'center', justifyContent: 'center' },
  content: { flexGrow: 1, padding: spacing.px16, gap: spacing.px12 },
  header: { gap: spacing.px12, marginBottom: spacing.px4 },
  addLink: { flexDirection: 'row', alignItems: 'center', alignSelf: 'flex-end', gap: spacing.px8 },
  addLinkText: { ...typography.label, color: colors.primary },
  pressed: { opacity: 0.7 },
});