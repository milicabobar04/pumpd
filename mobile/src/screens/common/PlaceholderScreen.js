import { View, Text, StyleSheet } from 'react-native';
import { theme } from '../../theme';

export default function PlaceholderScreen({ route}){
    const { opis, rute, faza } = route.params ?? {};
    return (
        <View style={styles.container}>
            <Text style={styles.title}>{opis ?? route.name}</Text>
            {rute ? <Text style={styles.caption}>API: {rute}</Text> : null}
            {faza ? <Text style={styles.caption}>Planirano: Faza {faza}</Text> : null}
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
    gap: theme.spacing.px8,
  },
  title: {
    ...theme.typography.sectionTitle,
    textAlign: 'center',
  },
  caption: {
    ...theme.typography.caption,
    textAlign: 'center',
  },
});