import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AppHeader } from '../components';
import MenuScreen from '../screens/common/MenuScreen';
import PlaceholderScreen from '../screens/common/PlaceholderScreen';

const Stack = createNativeStackNavigator();

const WORKER_MENU = [
  { label: 'Pregled svih članova', screen: 'WorkerMembers', icon: 'account-group', iconSet: 'mci' },
  { label: 'Pregled članarine', screen: 'WorkerMembership', icon: 'create-outline', boxed: true },
  { label: 'Pregled grupa', screen: 'WorkerGroups', icon: 'account-group', iconSet: 'mci' },
];

const WorkerHomeScreen = ({ navigation }) => (
  <MenuScreen navigation={navigation} items={WORKER_MENU} />
);

const screenOptions = {
  header: ({ navigation, back }) => <AppHeader navigation={navigation} showBack={!!back} />,
  animation: 'slide_from_right',
};

export default function WorkerStack() {
  return (
    <Stack.Navigator screenOptions={screenOptions}>
      <Stack.Screen name="WorkerHome" component={WorkerHomeScreen} options={{ title: 'Početna' }} />
      <Stack.Screen
        name="WorkerMembers"
        component={PlaceholderScreen}
        options={{ title: 'Pregled svih članova' }}
        initialParams={{ opis: 'Lista članova sa pretragom, detalji, dodavanje i brisanje', rute: '11.1–11.4', faza: 2 }}
      />
      <Stack.Screen
        name="WorkerMembership"
        component={PlaceholderScreen}
        options={{ title: 'Pregled članarine' }}
        initialParams={{ opis: 'Status, uređivanje i produženje članarine', rute: '11.5–11.7', faza: 3 }}
      />
      <Stack.Screen
        name="WorkerGroups"
        component={PlaceholderScreen}
        options={{ title: 'Pregled grupa' }}
        initialParams={{ opis: 'Pregled svih grupnih termina', rute: '9.1 (SU-43)', faza: 4 }}
      />
    </Stack.Navigator>
  );
}
