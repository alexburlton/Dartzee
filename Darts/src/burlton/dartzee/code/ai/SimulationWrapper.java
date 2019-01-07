package burlton.dartzee.code.ai;

import java.awt.Point;
import java.util.HashMap;

public class SimulationWrapper
{
	private double averageDart = 0;
	private double missPercent = 0;
	private double finishPercent = 0;
	private double treblePercent = 0;
	private HashMap<Point, Integer> hmPointToCount = new HashMap<>();
	
	public SimulationWrapper(double averageDart, double missPercent, double finishPercent, double treblePercent, HashMap<Point, Integer> hmPointToCount)
	{
		this.averageDart = averageDart;
		this.missPercent = missPercent;
		this.finishPercent = finishPercent;
		this.treblePercent = treblePercent;
		this.hmPointToCount = hmPointToCount;
	}
	
	public double getAverageDart()
	{
		return averageDart;
	}
	public double getFinishPercent()
	{
		return finishPercent;
	}
	public double getMissPercent()
	{
		return missPercent;
	}
	public double getTreblePercent()
	{
		return treblePercent;
	}
	public HashMap<Point, Integer> getHmPointToCount()
	{
		return hmPointToCount;
	}
}