package arun.com.chromer.webheads.tasks;

import de.jetwick.snacktory.HtmlFetcher;
import de.jetwick.snacktory.JResult;
import timber.log.Timber;

/**
 * Created by Arun on 15/05/2016.
 */
public class PageExtractRunnable implements Runnable {

    final PageExtractTaskMethods mPageTask;

    PageExtractRunnable(PageExtractTaskMethods pageTask) {
        mPageTask = pageTask;
    }

    interface PageExtractTaskMethods {

        /**
         * Sets the Thread that this instance is running on
         *
         * @param currentThread the current Thread
         */
        void setDownloadThread(Thread currentThread);

        JResult getResult();

        void setResult(JResult result);

        void handleDownloadState(int state);

        String getRawUrl();

        void setUnShortenedUrl(String url);

        String getUnShortenedUrl();
    }

    @Override
    public void run() {
        mPageTask.setDownloadThread(Thread.currentThread());

        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        try {
            cancelIfNeeded();

            HtmlFetcher fetcher = new HtmlFetcher();
            String url = fetcher.getResolvedUrl(mPageTask.getRawUrl(), 1000 * 10);
            mPageTask.setUnShortenedUrl(url);
            mPageTask.handleDownloadState(ExtractionTasksManager.URL_UN_SHORTENED);

            JResult res = fetcher.fetchAndExtract(url, 1000 * 10, false);
            cancelIfNeeded();

            mPageTask.setResult(res);
            mPageTask.handleDownloadState(ExtractionTasksManager.EXTRACTION_COMPLETE);

            cancelIfNeeded();
        } catch (InterruptedException ignore) {
            Timber.v("Thread interrupted");
        } catch (Exception e) {
            Timber.e(e.getMessage());
        }
    }

    private void cancelIfNeeded() throws InterruptedException {
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
    }
}
