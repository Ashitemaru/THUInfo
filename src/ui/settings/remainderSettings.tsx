import React, {useState} from "react";
import {getStr} from "../../utils/i18n";
import {SET_REMAINDER_SHIFT} from "../../redux/constants";
import {connect} from "react-redux";
import {Button, View, Text, TextInput} from "react-native";
import {getExpenditures} from "../../network/basics";
import Snackbar from "react-native-snackbar";
import {NetworkRetry} from "../../components/easySnackbars";

export const RemainderSettingsUI = ({
	setRemainderShift,
}: {
	setRemainderShift: (newShift: number) => void;
}) => {
	const [newValue, setNewValue] = useState("");

	return (
		<View style={{padding: 10}}>
			<View
				style={{
					padding: 8,
					paddingRight: 16,
					flexDirection: "row",
					justifyContent: "space-between",
					alignItems: "center",
				}}>
				<Text style={{fontSize: 17, flex: 4}}>{getStr("setNewRemainder")}</Text>
				<TextInput
					style={{
						fontSize: 15,
						flex: 1,
						backgroundColor: "white",
						textAlign: "left",
						borderColor: "lightgrey",
						borderWidth: 1,
						borderRadius: 5,
						padding: 6,
					}}
					value={newValue}
					onChangeText={setNewValue}
					keyboardType="numeric"
				/>
			</View>
			<View
				style={{
					flexDirection: "row",
					justifyContent: "space-around",
					alignItems: "center",
				}}>
				<Button
					title={getStr("confirm")}
					onPress={() => {
						setNewValue((value) => String(Number(value)));
						Snackbar.show({
							text: getStr("processing"),
							duration: Snackbar.LENGTH_SHORT,
						});
						getExpenditures(new Date(), new Date())
							.then((r) => {
								setRemainderShift(Number(newValue) - r[3]);
								Snackbar.show({
									text: getStr("success"),
									duration: Snackbar.LENGTH_SHORT,
								});
							})
							.catch(NetworkRetry);
					}}
				/>
				<Button
					title={getStr("resetRemainderShift")}
					onPress={() => {
						setRemainderShift(0);
						Snackbar.show({
							text: getStr("success"),
							duration: Snackbar.LENGTH_SHORT,
						});
					}}
				/>
			</View>
			<Text style={{padding: 10, marginTop: 10, textAlign: "center"}}>
				<Text style={{fontWeight: "bold", fontSize: 16, lineHeight: 18}}>
					{getStr("tips")}
				</Text>
				<Text style={{color: "gray", lineHeight: 18}}>
					{getStr("remainderTips")}
				</Text>
			</Text>
		</View>
	);
};

export const RemainderSettingsScreen = connect(undefined, (dispatch) => ({
	setRemainderShift: async (newShift: number) =>
		dispatch({type: SET_REMAINDER_SHIFT, payload: newShift}),
}))(RemainderSettingsUI);
