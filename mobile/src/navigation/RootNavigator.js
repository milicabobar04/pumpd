import React, { useEffect } from 'react';
import { View, ActivityIndicator, StyleSheet } from 'react-native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { theme } from '../theme';
import { useAuth } from '../context/AuthContext';
import { AppHeader, hasNotifications } from '../components';
import NotificationsScreen from '../screens/common/NotificationsScreen';
import SettingsScreen from '../screens/common/SettingsScreen';
import ReportIssueScreen from '../screens/public/ReportIssueScreen';
import AuthStack from './AuthStack';
import MemberTabs from './MemberTabs';
import TrainerTabs from './TrainerTabs';
import WorkerStack from './WorkerStack';
import AdminStack from './AdminStack';
import MyMembershipScreen from '../screens/member/MyMembershipScreen';
import GymOccupancyScreen from '../screens/member/GymOccupancyScreen';
import TrainerReviewsScreen from '../screens/trainer/TrainerReviewsScreen';

const Stack = createNativeStackNavigator();

const ROLE_NAVIGATORS = {
  MEMBER: MemberTabs,
  TRAINER: TrainerTabs,
  WORKER: WorkerStack,
  ADMIN: AdminStack,
};


const innerScreenOptions = {
  headerShown: true,
  animation: 'slide_from_right',
  header: ({ navigation }) => <AppHeader navigation={navigation} showBack />,
};

// Ekran obavezne promjene privremene lozinke: header bez nazad, zvonca i zupčanika,
// jer korisnik ne smije dalje dok ne promijeni lozinku. Odjava ostaje.
const forcedPasswordOptions = {
  headerShown: true,
  header: ({ navigation }) => <AppHeader navigation={navigation} minimal />,
};

// Bira šta se prikazuje na osnovu stanja iz AuthContext-a. Ekrani se ne mijenjaju
// sa navigation.navigate(...) poslije prijave ili odjave: kad se promijeni isSignedIn,
// role ili mustChangePassword, ovdje se renderuje drugi skup ekrana
// (preporučeni "auth flow" iz React Navigation dokumentacije).
//
//   1. isRestoring          -> spinner dok se čita sesija sa uređaja
//   2. nije prijavljen      -> AuthStack (prijava, informacije, prijava problema)
//   3. mustChangePassword   -> samo ekran promjene lozinke
//   4. prijavljen           -> dio aplikacije za ulogu + zajednički unutrašnji ekrani
export default function RootNavigator() {
  const { isSignedIn, isRestoring, role, mustChangePassword, signOut } = useAuth();
  const RoleTabs = ROLE_NAVIGATORS[role];

  // Prijavljen, ali uloga nije jedna od četiri poznate (pokvaren zapis ili izmjena ugovora):
  // briše se sesija, inače bi korisnik ostao zaglavljen bez ijednog ekrana.
  const unknownRole = isSignedIn && !RoleTabs;
  useEffect(() => {
    if (unknownRole) {
      console.warn(`Nepoznata uloga "${role}", sesija se briše.`);
      signOut();
    }
  }, [unknownRole, role, signOut]);

  if (isRestoring) {
    return (
      <View style={styles.loading}>
        <ActivityIndicator size="large" color={theme.colors.primary} accessibilityLabel="Učitavanje" />
      </View>
    );
  }

  return (
    <Stack.Navigator screenOptions={{ headerShown: false, animation: 'fade' }}>
      {isSignedIn && RoleTabs && mustChangePassword ? (
        <Stack.Screen
          name="ForcedPasswordChange"
          component={SettingsScreen}
          options={forcedPasswordOptions}
        />
      ) : isSignedIn && RoleTabs ? (
        <>
          <Stack.Screen name={`App_${role}`} component={RoleTabs} />

          <Stack.Group screenOptions={innerScreenOptions}>
            {hasNotifications(role) ? (
              <Stack.Screen
                name="Notifications"
                component={NotificationsScreen}
                options={{ title: 'Obavještenja' }}
              />
            ) : null}

            {role === 'MEMBER' ? (
              <>
                <Stack.Screen name="MyMembership" component={MyMembershipScreen} options={{ title: 'Moja članarina' }} />
                <Stack.Screen name="GymOccupancy" component={GymOccupancyScreen} options={{ title: 'Zauzetost teretane' }} />
              </>
            ) : null}

            {role === 'TRAINER' ? (
              <Stack.Screen name="TrainerReviews" component={TrainerReviewsScreen} options={{ title: 'Moje recenzije' }} />
            ) : null}

            <Stack.Screen
              name="Settings"
              component={SettingsScreen}
              options={{ title: 'Podešavanja naloga' }}
            />
            <Stack.Screen
              name="ReportIssue"
              component={ReportIssueScreen}
              options={{ title: 'Prijava problema' }}
              initialParams={{ embedded: true }}
            />
          </Stack.Group>
        </>
      ) : (
        <Stack.Screen name="Auth" component={AuthStack} />
      )}
    </Stack.Navigator>
  );
}

const styles = StyleSheet.create({
  loading: {
    flex: 1,
    backgroundColor: theme.colors.background,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
