import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { NavigationContainer } from '@react-navigation/native';
import { Text, View } from 'react-native';

import CatalogScreen from '../screens/CatalogScreen';
import DetailScreen from '../screens/DetailScreen';
import CartScreen from '../screens/CartScreen';
import LibraryScreen from '../screens/LibraryScreen';
import LoginScreen from '../screens/LoginScreen';
import { useCart } from '../CartContext';

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator();

function TabNavigator() {
    const { count } = useCart();
    return (
        <Tab.Navigator
            screenOptions={({ route }) => ({
                tabBarIcon: ({ color, size }) => {
                    let iconName;
                    if (route.name === 'CatalogTab') iconName = 'Explore';
                    else if (route.name === 'CartTab') iconName = 'Cart';
                    else if (route.name === 'LibraryTab') iconName = 'Library';

                    return (
                        <View>
                            <Text style={{ color, fontSize: 12, fontWeight: 'bold' }}>{iconName}</Text>
                            {route.name === 'CartTab' && count > 0 && (
                                <View style={{ position: 'absolute', right: -15, top: -5, backgroundColor: 'red', borderRadius: 10, width: 20, height: 20, justifyContent: 'center', alignItems: 'center' }}>
                                    <Text style={{ color: 'white', fontSize: 10, fontWeight: 'bold' }}>{count}</Text>
                                </View>
                            )}
                        </View>
                    );
                },
                tabBarActiveTintColor: '#1f2838',
                tabBarInactiveTintColor: '#aaa',
                tabBarShowLabel: false,
                headerShown: false,
                tabBarStyle: { height: 60, paddingBottom: 10 }
            })}
        >
            <Tab.Screen name="CatalogTab" component={CatalogScreen} />
            <Tab.Screen name="CartTab" component={CartScreen} />
            <Tab.Screen name="LibraryTab" component={LibraryScreen} />
        </Tab.Navigator>
    );
}

export default function AppNavigator() {
    return (
        <NavigationContainer>
            <Stack.Navigator screenOptions={{ headerShown: false }}>
                <Stack.Screen name="Main" component={TabNavigator} />
                <Stack.Screen name="Detail" component={DetailScreen} options={{ headerShown: true, title: 'Product Details', headerBackTitleVisible: false, headerTintColor: '#1f2838' }} />
                <Stack.Screen name="Login" component={LoginScreen} options={{ presentation: 'modal' }} />
            </Stack.Navigator>
        </NavigationContainer>
    );
}
