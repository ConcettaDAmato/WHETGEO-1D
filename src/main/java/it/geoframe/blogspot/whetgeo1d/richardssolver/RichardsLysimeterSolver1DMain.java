/*
 * GNU GPL v3 License
 *
 * Copyright 2019 Concetta D'Amato
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package it.geoframe.blogspot.whetgeo1d.richardssolver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import it.geoframe.blogspot.closureequation.conductivitymodel.ConductivityEquation;
import it.geoframe.blogspot.closureequation.conductivitymodel.ConductivityEquationFactory;
import it.geoframe.blogspot.closureequation.conductivitymodel.UnsaturatedHydraulicConductivityTemperatureFactory;
import it.geoframe.blogspot.closureequation.interfaceconductivity.InterfaceConductivity;
import it.geoframe.blogspot.closureequation.interfaceconductivity.SimpleInterfaceConductivityFactory;
import it.geoframe.blogspot.closureequation.closureequation.ClosureEquation;
import it.geoframe.blogspot.closureequation.closureequation.Parameters;
import it.geoframe.blogspot.closureequation.closureequation.SoilWaterRetentionCurveFactory;
import it.geoframe.blogspot.closureequation.equationstate.EquationState;
import it.geoframe.blogspot.whetgeo1d.data.ComputeQuantitiesLysimeter;
import it.geoframe.blogspot.whetgeo1d.data.ComputeQuantitiesRichards;
import it.geoframe.blogspot.whetgeo1d.data.Geometry;
import it.geoframe.blogspot.whetgeo1d.data.ProblemQuantities;
import it.geoframe.blogspot.whetgeo1d.equationstate.EquationStateFactory;
import it.geoframe.blogspot.whetgeo1d.equationstate.WaterDepth;
import oms3.annotations.*;





@Description("Solve the Richards equation for the 1D domain.")
@Documentation("")
@Author(name = "Concetta D'Amato, Niccolo' Tubini, and Riccardo Rigon", contact = "")
@Keywords("Hydrology, Richards, Infiltration")
@Bibliography("Casulli (2010)")
//@Label()
//@Name()
//@Status()
@License("General Public License Version 3 (GPLv3)")
public class RichardsLysimeterSolver1DMain {

	/*
	 * SOIL PARAMETERS
	 */
	@Description("The hydraulic conductivity at saturation")
	@In 
	@Unit ("m/s")
	public double[] ks;

	@Description("Saturated water content")
	@In 
	@Unit ("-")
	public double[] thetaS;

	@Description("Residual water content")
	@In 
	@Unit ("-")
	public double[] thetaR;

	@Description("Water content at the whilting point")
	@In 
	@Unit ("-")
	public double[] thetaWP;

	@Description("Water content at field capacity")
	@In 
	@Unit ("-")
	public double[] thetaFC;

	@Description("First parameter of SWRC")
	@In 
	@Unit ("-")
	public double[] par1SWRC;

	@Description("Second parameter of SWRC")
	@In 
	@Unit ("-")
	public double[] par2SWRC;

	@Description("Third parameter of SWRC")
	@In 
	@Unit ("-")
	public double[] par3SWRC;

	@Description("Fourth parameter of SWRC")
	@In 
	@Unit ("-")
	public double[] par4SWRC;

	@Description("Fifth parameter of SWRC")
	@In 
	@Unit ("-")
	public double[] par5SWRC;

	@Description("Aquitard compressibility")
	@In 
	@Unit ("1/Pa")
	public double[] alphaSpecificStorage;

	@Description("Water compressibility")
	@In 
	@Unit ("1/Pa")
	public double[] betaSpecificStorage;

	@Description("Coefficient for water suction dependence on temperature")
	@In 
	@Unit ("K")
	public double beta0 = -776.45;

	@Description("Reference temperature for soil water content")
	@In 
	@Unit ("K")
	public double temperatureR = 278.15;
	
	@Description("Control volume label defining the rheology")
	@In 
	@Unit("-")
	public int[] inRheologyID;

	@Description("Control volume label defining the set of the paramters")
	@In 
	@Unit("-")
	public int[] inParameterID;
	
	/*
	 * MODELS
	 * - closure equation
	 * - conductivity model
	 * - interface conductivity model
	 */
	@Description("It is possibile to chose between 3 different models to compute "
			+ "the soil hydraulic properties: Van Genuchten; Brooks and Corey; Kosugi unimodal")
	@In 
	public String soilHydraulicModel;

	@Description("It is possible to choose among these models:"
			+ "notemperature, ....")
	@In 
	public String typeUHCTemperatureModel;

	@Description("It is possible to choose among these models:"
			+ "Mualem Van Genuchten, Mualem Brooks Corey, ....")
	@In 
	public String typeUHCModel;

	@Description("Hydraulic conductivity at control volume interface can be evaluated as"
			+ " the average of kappas[i] and kappas[i+1]"
			+ " the maximum between kappas[i] and kappas[i+1]"
			+ " the minimum between kappas[i] and kappas[i+1]"
			+ " a weighted average of kappas[i] and kappas[i+1] where weights are dx[i] and dx[i+1]")
	@In
	public String interfaceHydraulicConductivityModel;


	/*
	 * INITIAL CONDITION
	 */
	@Description("Initial condition for water suction read from grid NetCDF file")
	@In
	@Unit("m")
	public double[] psiIC;

	@Description("Initial condition for temperature read from grid NetCDF file")
	@In
	@Unit("K")
	public double[] temperature;

	/*
	 * GEOMETRY
	 */
	@Description("z coordinate read from grid NetCDF file")
	@In
	@Unit("m")
	public double[] z;

	@Description("Space delta to compute gradients read from grid NetCDF file")
	@In 
	@Unit("m")
	public double[] spaceDeltaZ;

	@Description("Length of control volumes read from grid NetCDF file")
	@In 
	@Unit("m")
	public double[] controlVolume;

	@Description("Maximum ponding depth")
	@In 
	@Unit("m")
	public double maxPonding;

	/*
	 * TIME STEP
	 */
	@Description("Time amount at every time-loop")
	@In
	@Unit ("s")
	public double tTimeStep;

	@Description("Time step of integration")
	@In
	@Unit ("s")
	public double timeDelta;

	/*
	 * ITERATION PARAMETERS
	 */
	@Description("Tolerance for Newton iteration")
	@In
	public double newtonTolerance;

	@Description("Control parameter for nested Newton algorithm:"
			+"0 --> simple Newton method"
			+"1 --> nested Newton method")
	@In
	public int nestedNewton; 

	@Description("Damped factor for Newton algorithm")
	@In
	public double delta = 0.0; 
	
	@Description("Number of Picard iteration to update the diffusive flux matrix")
	@In
	public int picardIteration=1;

	/*
	 *  BOUNDARY CONDITIONS
	 */

	@Description("The HashMap with the time series of the boundary condition at the top of soil column")
	@In
	@Unit ("m")
	public HashMap<Integer, double[]> inTopBC;

	@Description("It is possibile to chose between 2 different kind "
			+ "of boundary condition at the top of the domain: "
			+ "- Dirichlet boundary condition --> Top Dirichlet"
			+ "- Neumann boundary condition --> Top Neumann")
	@In 
	public String topBCType;

	@Description("The HashMap with the time series of the boundary condition at the bottom of soil column")
	@In
	@Unit ("m")
	public HashMap<Integer, double[]> inBottomBC;

	@Description("")
	@In
	@Unit ("")
	public HashMap<Integer, double[]> inSaveDate;

	@Description("It is possibile to chose among 3 different kind "
			+ "of boundary condition at the bottom of the domain: "
			+ "- Dirichlet boundary condition --> Bottom Dirichlet"
			+ "- Neumann boundary condition --> Bottom Neumann"
			+ "- Impervious boundary condition --> Bottom Impervious")
	@In 
	public String bottomBCType;

	@Description("The current date of the simulation.")
	@In
	@Out
	public String inCurrentDate;
	
	/*
	 * LYSIMETER
	 */

	@Description("Stressed Evapotranspiration for each layer")
	@In
	@Unit("mm")
	public double[] stressedETs;

	/*
	 * OUTPUT
	 */
	@Description("ArrayList of variable to be stored in the buffer writer")
	@Out
	public ArrayList<double[]> outputToBuffer;
	
	@Description("Soil water content at the new time level. This will be passed to the Broker component")
	@Out
	public double[] thetasNew;
	


	@Description("Control variable")
	@Out
	public boolean doProcessBuffer;
	
	//////////////////////////////////////////
	//////////////////////////////////////////
	

	@Description("Maximun number of Newton iterations")
	private final int MAXITER_NEWT = 50;

	@Description("Number of control volume for domain discetrization")
	@Unit (" ")
	private int KMAX; 

	@Description("It is needed to iterate on the date")
	private int step;

	@Description("Control value to save output:"
			+ "- 1 save the current time step output"
			+ "- 0 do not save")
	private double saveDate;


	private PDE1DSolver richardsSolver;
	private ProblemQuantities variables;
	private Geometry geometry;
	private Parameters rehologyParameters;
	private ComputeQuantitiesRichards computeQuantitiesRichards;
	private ComputeQuantitiesLysimeter computeQuantitiesLysimeter;
	
	@Execute
	public void solve() {



		if(step==0){
			KMAX = psiIC.length;

			variables = ProblemQuantities.getInstance(psiIC, temperature, inRheologyID, inParameterID);
			geometry = Geometry.getInstance(z, spaceDeltaZ, controlVolume);
			rehologyParameters = Parameters.getInstance(1000.0, 970, 4188,
					2117, 0.6, 2.29, 333700, 273.15,
					thetaS, thetaR, new double[] {-9999.0}, new double[] {-9999.0}, new double[] {-9999.0},
					new double[] {-9999.0}, par1SWRC, par2SWRC, par3SWRC, par4SWRC, par5SWRC, ks, alphaSpecificStorage, betaSpecificStorage);

			computeQuantitiesRichards = new ComputeQuantitiesRichards(soilHydraulicModel, typeUHCModel, typeUHCTemperatureModel, interfaceHydraulicConductivityModel, topBCType, bottomBCType);
			computeQuantitiesLysimeter = new ComputeQuantitiesLysimeter(thetaWP, thetaFC);
			
			outputToBuffer = new ArrayList<double[]>();

			List<EquationState> equationState = computeQuantitiesRichards.getRichardsStateEquation();
			
			richardsSolver = new PDE1DSolver(topBCType, bottomBCType, KMAX, nestedNewton, newtonTolerance, delta, MAXITER_NEWT, equationState);

			if(topBCType.equalsIgnoreCase("Top Dirichlet")||topBCType.equalsIgnoreCase("TopDirichlet")) {
				KMAX = KMAX-1;
			}

		} // close step==0

		doProcessBuffer = false;


		variables.richardsTopBCValue = 0.0;
		if(topBCType.equalsIgnoreCase("Top Neumann") || topBCType.equalsIgnoreCase("TopNeumann")) {
			variables.richardsTopBCValue = (inTopBC.get(0)[0]/1000)/tTimeStep;
		} else {
			variables.richardsTopBCValue = inTopBC.get(0)[0]/1000;
		}
		

		variables.richardsBottomBCValue = 0.0;
		if(inBottomBC != null) {
			variables.richardsBottomBCValue = inBottomBC.get(0)[0];
		}

		saveDate = -1.0;
		saveDate = inSaveDate.get(0)[0];
		
		computeQuantitiesLysimeter.computeEvapoTranspirations(KMAX, tTimeStep, stressedETs);

		computeQuantitiesRichards.resetRunOff();
		
		outputToBuffer.clear();

		double sumTimeDelta = 0;

		while(sumTimeDelta < tTimeStep) {


			if(sumTimeDelta + timeDelta>tTimeStep) {
				timeDelta = tTimeStep - sumTimeDelta;
			}
			sumTimeDelta = sumTimeDelta + timeDelta;



			/*
			 * Compute water volumes
			 */
			computeQuantitiesRichards.computeWaterVolume(KMAX);

			
			/*
			 * Compute xStar
			 */
			computeQuantitiesRichards.computeXStar(KMAX);
			
			
			/*
			 * Check the sink term for ET
			 */
			computeQuantitiesLysimeter.checkEvapoTranspirations(KMAX, timeDelta);

			
			/*
			 * Solve PDE
			 */
			for(int picard=0; picard<picardIteration; picard++) {

				/*
				 * Compute hydraulic conductivity
				 * 
				 */	
				computeQuantitiesRichards.computeHydraulicConductivity(KMAX);
				computeQuantitiesRichards.computeInterfaceHydraulicConductivity(KMAX);
				
				
				/*
				 * Solve PDE
				 */
				richardsSolver.solve(timeDelta, KMAX);

			} // close Picard iteration
			

			/*
			 * compute run-off
			 */
			computeQuantitiesRichards.computeRunOff(KMAX, maxPonding);
			

			/*
			 * Compute 
			 * - water volume and total water volume
			 * - water content
			 */
			computeQuantitiesRichards.computeWaterVolumeNew(KMAX);
			computeQuantitiesRichards.computeThetasNew(KMAX);
			

			/*
			 * Fluxes
			 */
			computeQuantitiesRichards.computeDarcyVelocities(KMAX);
			computeQuantitiesRichards.computeDarcyVelocitiesCapillary(KMAX);
			computeQuantitiesRichards.computeDarcyVelocitiesGravity(KMAX);
			computeQuantitiesRichards.computePoreVelocities(KMAX);
			computeQuantitiesRichards.computeCelerities(KMAX);
			computeQuantitiesRichards.computeKinematicRatio(KMAX);

			
			/*
			 * compute error
			 */
			computeQuantitiesRichards.computeError(KMAX, timeDelta);

		}

		thetasNew = variables.thetasNew;
		
		if(saveDate == 1) {
			outputToBuffer.add(variables.waterSuctions);
			outputToBuffer.add(variables.thetasNew);
			outputToBuffer.add(variables.volumesNew);
			outputToBuffer.add(variables.darcyVelocities);
			outputToBuffer.add(variables.darcyVelocitiesCapillary);
			outputToBuffer.add(variables.darcyVelocitiesGravity);
			outputToBuffer.add(variables.poreVelocities);
			outputToBuffer.add(variables.celerities);
			outputToBuffer.add(variables.kinematicRatio);
			outputToBuffer.add(variables.ETs);
			outputToBuffer.add(new double[] {variables.errorVolume});
			outputToBuffer.add(new double[] {variables.richardsTopBCValue*tTimeStep*1000}); // I want to have rainfall height instead of water flux
			outputToBuffer.add(new double[] {variables.richardsBottomBCValue});
			outputToBuffer.add(new double[] {variables.runOff/tTimeStep}); // surface runoff
			doProcessBuffer = true;
		} else {
			//			System.out.println("SaveDate = " + saveDate);
		}
		step++;

	} //// MAIN CYCLE END ////

}  /// CLOSE Richards1d ///


