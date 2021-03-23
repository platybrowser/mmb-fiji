package de.embl.cba.mobie2.json;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.embl.cba.mobie2.Project;
import de.embl.cba.tables.FileAndUrlUtils;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class ProjectJsonParser
{
	public Project getProject( String path ) throws IOException
	{
		final String s = FileAndUrlUtils.read( path );
		Gson gson = new Gson();
		Type type = new TypeToken< Project >() {}.getType();
		Project project = gson.fromJson( s, type );
		return project;
	}
}