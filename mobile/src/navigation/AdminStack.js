import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AppHeader } from '../components';
import MenuScreen from '../screens/common/MenuScreen';
import PlaceholderScreen from '../screens/common/PlaceholderScreen';
import TrainersScreen from '../screens/admin/TrainersScreen';
import TrainerDetailsScreen from '../screens/admin/TrainerDetailsScreen';
import AddTrainerScreen from '../screens/admin/AddTrainerScreen';
import WorkersScreen from '../screens/admin/WorkersScreen';
import WorkerDetailsScreen from '../screens/admin/WorkerDetailsScreen';
import AddWorkerScreen from '../screens/admin/AddWorkerScreen';



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
      <Stack.Screen name="AdminTrainers" component={TrainersScreen} options={{ title: 'Pregled svih trenera' }} />
      <Stack.Screen name="AdminTrainerDetails" component={TrainerDetailsScreen} options={{ title: 'Detalji trenera' }} />
      <Stack.Screen name="AdminAddTrainer" component={AddTrainerScreen} options={{ title: 'Dodavanje trenera' }} />
      <Stack.Screen name="AdminWorkers" component={WorkersScreen} options={{ title: 'Pregled svih radnika' }} />
      <Stack.Screen name="AdminWorkerDetails" component={WorkerDetailsScreen} options={{ title: 'Detalji radnika' }} />
      <Stack.Screen name="AdminAddWorker" component={AddWorkerScreen} options={{ title: 'Dodavanje radnika' }} />

      <Stack.Screen
        name="AdminIssues"
        component={PlaceholderScreen}
        options={{ title: 'Obrada problema' }}
        initialParams={{ opis: 'Prijavljeni problemi i promjena statusa', rute: '4.2, 4.3', faza: 2 }}
      />
    </Stack.Navigator>
  );
}
