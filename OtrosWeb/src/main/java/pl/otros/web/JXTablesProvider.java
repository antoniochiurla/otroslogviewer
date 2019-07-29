package pl.otros.web;

import org.jdesktop.swingx.JXTable;

public interface JXTablesProvider {

	int getCount();
	JXTable getJXTable( int index);
}
