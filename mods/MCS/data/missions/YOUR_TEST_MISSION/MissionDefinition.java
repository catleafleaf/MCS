package data.missions.YOUR_TEST_MISSION;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.ids.StarTypes;
import com.fs.starfarer.api.loading.VariantSource;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
public class MissionDefinition implements MissionDefinitionPlugin {
	@Override
	public void defineMission(MissionDefinitionAPI api) {

		api.initFleet(FleetSide.PLAYER, "THIS IS", FleetGoal.ATTACK, false);
		api.initFleet(FleetSide.ENEMY, "A TEST", FleetGoal.ATTACK, true);
		api.setFleetTagline(FleetSide.PLAYER, "A");
		api.setFleetTagline(FleetSide.ENEMY, "B");

		api.addBriefingItem("TEST A");

		// Set up the player's fleet.
		api.addToFleet(FleetSide.PLAYER, "MCS_Creeper_variant", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "MCS_Evoker_variant", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "MCS_IronGolem_variant", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "MCS_Phantom_variant", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "MCS_Skeleton_variant", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "MCS_Vindicator_variant", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "MCS_Spider_variant", FleetMemberType.SHIP, false);
		api.addToFleet(FleetSide.PLAYER, "MCS_Phantom_variant", FleetMemberType.SHIP, false);

		// Set up the enemy fleet.
		api.addToFleet(FleetSide.ENEMY, "station1_Standard", FleetMemberType.SHIP, false);


		// Set up the map.
		float width = 10000f;
		float height = 10000f;
		api.initMap(-width / 2f, width / 2f, -height / 2f, height / 2f);
		api.setBackgroundSpriteName("graphics/backgrounds/background1.jpg");
		float minX = -width / 2;
		float minY = -height / 2;
		api.addAsteroidField(minX, minY + height / 2f, 0f, 4000f, 5f, 50f, 50);
		api.addPlanet(-500f, 500f, 100f, StarTypes.YELLOW, 50f, true);
		api.addNebula(-400, 2100, 200f);
	}
}