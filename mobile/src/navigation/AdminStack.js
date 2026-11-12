import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AppHeader } from '../components';
import MenuScreen from '../screens/common/MenuScreen';
import PlaceholderScreen from '../screens/common/PlaceholderScreen';

const Stack = createNativeStackNavigator();

const ADMIN_MENU = [
  { label: 'Uređivanje informacija', screen: 'AdminGymInfo', icon: 'create-outline', boxed: true },
  { label: 'Pregled svih trenera', screen: 'AdminTrainers', icon: 'account-group', iconSet: 'mci' },
  { label: 'Pregled svih radnika', screen: 'AdminWorkers', icon: 'account-group', iconSet: 'mci' },
  { label: 'Obrada problema', screen: 'AdminIssues', icon: 'build-outline' },
];

const AdminHomeScreen = ({ navigation }) => (
  <MenuScreen navigation={navigation} items={ADMIN_MENU} />
);

const screenOptions = {
  header: ({ navigation, back }) => <AppHeader navigation={navigation} showBack={!!back} />,
  animation: 'slide_from_right',
};

export default function AdminStack() {
  return (
    <Stack.Navigator screenOptions={screenOptions}>
      <Stack.Screen name="AdminHome" component={AdminHomeScreen} options={{ title: 'Početna' }} />
      <Stack.Screen
        name="AdminGymInfo"
        component={PlaceholderScreen}
        options={{ title: 'Uređivanje informacija' }}
        initialParams={{ opis: 'Informacije o teretani', rute: '5.1, 5.2', faza: 2 }}
      />
      <Stack.Screen
        name="AdminTrainers"
        component={PlaceholderScreen}
        options={{ title: 'Pregled svih trenera' }}
        initialParams={{ opis: 'Lista, detalji, dodavanje i brisanje trenera', rute: '12.1–12.4', faza: 2 }}
      />
      <Stack.Screen
        name="AdminWorkers"
        component={PlaceholderScreen}
        options={{ title: 'Pregled svih radnika' }}
        initialParams={{ opis: 'Lista, detalji, dodavanje i brisanje radnika', rute: '12.5–12.8', faza: 2 }}
      />
      <Stack.Screen
        name="AdminIssues"
        component={PlaceholderScreen}
        options={{ title: 'Obrada problema' }}
        initialParams={{ opis: 'Prijavljeni problemi i promjena statusa', rute: '4.2, 4.3', faza: 2 }}
      />
    </Stack.Navigator>
  );
}
