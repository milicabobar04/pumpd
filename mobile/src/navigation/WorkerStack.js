import React from 'react';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { AppHeader } from '../components';
import MenuScreen from '../screens/common/MenuScreen';
import PlaceholderScreen from '../screens/common/PlaceholderScreen';
import MembersScreen from '../screens/worker/MembersScreen';
import MemberDetailsScreen from '../screens/worker/MemberDetailsScreen';
import AddMemberScreen from '../screens/worker/AddMemberScreen';
import MemberMembershipScreen from '../screens/worker/MemberMembershipScreen';
import ScanQrScreen from '../screens/worker/ScanQrScreen';

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
      <Stack.Screen name="WorkerMembers" component={MembersScreen} options={{ title: 'Pregled svih članova' }} />
      <Stack.Screen name="WorkerMemberDetails" component={MemberDetailsScreen} options={{ title: 'Detalji člana' }} />
      <Stack.Screen name="WorkerAddMember" component={AddMemberScreen} options={{ title: 'Dodavanje člana' }} />
      <Stack.Screen
        name="WorkerMembership"
        component={MembersScreen}
        options={{ title: 'Pregled članarine' }}
        initialParams={{ mode: 'membership' }}
      />
      <Stack.Screen name="WorkerMemberMembership" component={MemberMembershipScreen} options={{ title: 'Članarina' }} />
      <Stack.Screen name="WorkerScanQr" component={ScanQrScreen} options={{ title: 'Skeniranje QR koda' }} />
      <Stack.Screen
        name="WorkerGroups"
        component={PlaceholderScreen}
        options={{ title: 'Pregled grupa' }}
        initialParams={{ opis: 'Pregled svih grupnih termina', rute: '9.1 (SU-43)', faza: 4 }}
      />
    </Stack.Navigator>
  );
}
