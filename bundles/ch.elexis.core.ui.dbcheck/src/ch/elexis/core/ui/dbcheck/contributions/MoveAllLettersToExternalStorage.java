package ch.elexis.core.ui.dbcheck.contributions;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.IProgressMonitor;

import ch.elexis.core.l10n.Messages;
import ch.elexis.core.model.IDocumentLetter;
import ch.elexis.core.model.util.DocumentLetterUtil;
import ch.elexis.core.services.IQuery;
import ch.elexis.core.services.IQueryCursor;
import ch.elexis.core.services.IVirtualFilesystemService.IVirtualFilesystemHandle;
import ch.elexis.core.services.holder.CoreModelServiceHolder;
import ch.elexis.core.ui.dbcheck.external.ExternalMaintenance;

public class MoveAllLettersToExternalStorage extends ExternalMaintenance {
	
	public MoveAllLettersToExternalStorage(){}
	
	@Override
	public String executeMaintenance(IProgressMonitor pm, String DBVersion){
		
		StringBuilder result = new StringBuilder();
		int okCount = 0;
		int failCount = 0;
		IQuery<IDocumentLetter> query =
			CoreModelServiceHolder.get().getQuery(IDocumentLetter.class);
		try (IQueryCursor<IDocumentLetter> letters = query.executeAsCursor()) {
			pm.beginTask(Messages.Texterstellung_save_all_letters_externally, letters.size());
			while (letters.hasNext()) {
				IDocumentLetter letter = letters.next();
				try {
					boolean ok = exportToExtern(letter, result);
					if (ok) {
						okCount++;
					} else {
						failCount++;
						result.append("[" + letter.getId() + "] could not export: see log\n");
					}
					pm.worked(1);
				} catch (IOException e) {
					failCount++;
					result.append(
						"[" + letter.getId() + "] could not export: " + e.getMessage() + "\n");
				}
			}
		}
		pm.done();
		return okCount + " OK / " + failCount + " FAIL\n" + result.toString();
	}
	
	@Override
	public String getMaintenanceDescription(){
		return "[EXPLETT] Export all letters from DB storage to external storage";
	}
	
	/**
	 * Try to save the {@link IDocumentLetter} on external storage. Extern file configuration has to
	 * be valid.
	 * @param result 
	 * 
	 * @param brief
	 * @return
	 * @throws IOException
	 */
	public boolean exportToExtern(IDocumentLetter letter, StringBuilder result) throws IOException{
		IVirtualFilesystemHandle externalHandle =
			DocumentLetterUtil.getExternalHandleIfApplicable(letter);
		if (externalHandle != null && externalHandle.exists() && externalHandle.canRead()) {
			return true;
		} else {
			byte[] content = null;
			try (InputStream letterInputStream = letter.getContent()) {
				if (letterInputStream != null) {
					content = IOUtils.toByteArray(letterInputStream);
				} else {
					result.append("[" + letter.getId() + "] content is null\n");
				}
			}
			if (content != null) {
				try (InputStream is = new ByteArrayInputStream(content)) {
					letter.setContent(is);
				}
			}
			
			return true;
		}
	}
	
}
