import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AppHeader } from '../components';
import MenuScreen from '../screens/common/MenuScreen';
import PlaceholderScreen from '../screens/common/PlaceholderScreen';
import PlansScreen from '../screens/plans/PlansScreen';
import PlanDetailsScreen from '../screens/plans/PlanDetailsScreen';
import PlanFormScreen from '../screens/plans/PlanFormScreen';

const Stack = createNativeStackNavigator();

const screenOptions = {
  header: ({ navigation, back }) => <AppHeader navigation={navigation} showBack={!!back} />,
  animation: 'slide_from_right',
};


const planScreens = (
  <>
    <Stack.Screen name="Plans" component={PlansScreen} options={{ title: 'Planovi treninga' }} />
    <Stack.Screen name="PlanDetails" component={PlanDetailsScreen} options={{ title: 'Plan treninga' }} />
    <Stack.Screen name="PlanForm" component={PlanFormScreen} options={{ title: 'Plan treninga' }} />
  </>
);

const MEMBER_TRAININGS_MENU = [
  { label: 'Planovi treninga', screen: 'Plans', icon: 'clipboard-text-outline', iconSet: 'mci' },
  { label: 'Grupni termini', screen: 'MemberGroupSessions', icon: 'account-group', iconSet: 'mci' },
  { label: 'Istorija treninga', screen: 'MemberHistory', icon: 'history', iconSet: 'mci' },
];

const MemberTrainingsHome = ({ navigation }) => (
  <MenuScreen navigation={navigation} items={MEMBER_TRAININGS_MENU} />
);

export function MemberTrainingsStack() {
  return (
    <Stack.Navigator screenOptions={screenOptions}>
      <Stack.Screen name="MemberTrainingsHome" component={MemberTrainingsHome} options={{ title: 'Treninzi' }} />
      {planScreens}
      <Stack.Screen
        name="MemberGroupSessions"
        component={PlaceholderScreen}
        options={{ title: 'Grupni termini' }}
        initialParams={{ opis: 'Grupni termini i prijava', rute: '9.1–9.3', faza: 4 }}
      />
      <Stack.Screen
        name="MemberHistory"
        component={PlaceholderScreen}
        options={{ title: 'Istorija treninga' }}
        initialParams={{ opis: 'Prethodni treninzi', rute: '2.4', faza: 4 }}
      />
    </Stack.Navigator>
  );
}

export function TrainerPlansStack() {
  return (
    <Stack.Navigator screenOptions={screenOptions}>
      {planScreens}
    </Stack.Navigator>
  );
}
