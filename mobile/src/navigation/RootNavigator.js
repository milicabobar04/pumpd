import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useAuth } from '../context/AuthContext';
import { AppHeader, hasNotifications } from '../components';
import PlaceholderScreen from '../screens/common/PlaceholderScreen';
import SettingsScreen from '../screens/common/SettingsScreen';
import AuthStack from './AuthStack';
import MemberTabs from './MemberTabs';
import TrainerTabs from './TrainerTabs';
import WorkerStack from './WorkerStack';
import AdminStack from './AdminStack';

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

export default function RootNavigator() {
  const { isSignedIn, role } = useAuth();
  const RoleTabs = ROLE_NAVIGATORS[role];

  return (
    <Stack.Navigator screenOptions={{ headerShown: false, animation: 'fade' }}>
      {isSignedIn && RoleTabs ? (
        <>
          <Stack.Screen name={`App_${role}`} component={RoleTabs} />

          <Stack.Group screenOptions={innerScreenOptions}>
            {hasNotifications(role) ? (
              <Stack.Screen
                name="Notifications"
                component={PlaceholderScreen}
                options={{ title: 'Obavještenja' }}
                initialParams={{ opis: 'Lista obavještenja', rute: '6.1', faza: 1 }}
              />
            ) : null}
            <Stack.Screen
              name="Settings"
              component={SettingsScreen}
              options={{ title: 'Podešavanja naloga' }}
            />
          </Stack.Group>
        </>
      ) : (
        <Stack.Screen name="Auth" component={AuthStack} />
      )}
    </Stack.Navigator>
  );
}
